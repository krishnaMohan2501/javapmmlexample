package pmml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.xml.bind.JAXBException;
import javax.xml.transform.sax.SAXSource;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.MiningModelEvaluator;
import org.jpmml.evaluator.ProbabilityClassificationMap;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  Class ia responsible for traversing decision tree and computing the result.
 */

public class App {

    public App(){}

    public static void main(String... args) throws Exception {
        App app = new App();
        app.run(args);
    }

    /**
     * Main script which parse decision tree and compute final score
     * @param args
     * @throws Exception
     */
    public void run(String ... args) throws Exception {

        //configuring model in decision_tree.pmml
        PMML pmml = createPMMLfromFile("decision_tree.pmml");

        ModelEvaluator<MiningModel> modelEvaluator = new MiningModelEvaluator(pmml);
        printArgumentsOfModel(modelEvaluator);

        //taking input from csv file
        List<String> dataLines = Files.readAllLines(Paths.get(App.class.getResource("input.csv").toURI()));

        for(String dataLine : dataLines){
            if(dataLine.startsWith("temperature")) continue;

            Map<FieldName, FieldValue> arguments = readArgumentsFromLine(dataLine, modelEvaluator);

            modelEvaluator.verify();

            Map<FieldName, ?> results = modelEvaluator.evaluate(arguments);

            FieldName targetName = modelEvaluator.getTargetField();
            Object targetValue = results.get(targetName);

            ProbabilityClassificationMap nodeMap = (ProbabilityClassificationMap) targetValue;

            System.out.println("== Result: " + nodeMap.getResult() +"\n");
        }
    }

    public Map<FieldName, FieldValue> readArgumentsFromLine(String line, ModelEvaluator<MiningModel> modelEvaluator) {
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();
        String[] lineArgs = line.split(",");

        if( lineArgs.length != 4) return arguments;

        FieldValue temperature = modelEvaluator.prepare(new FieldName("temperature"), lineArgs[0].isEmpty() ? 0 : lineArgs[0]);
        FieldValue humidity = modelEvaluator.prepare(new FieldName("humidity"), lineArgs[1].isEmpty() ? 0 : lineArgs[1]);
        FieldValue windy = modelEvaluator.prepare(new FieldName("windy"), lineArgs[2].isEmpty() ? "false" : lineArgs[2]);
        FieldValue outlook = modelEvaluator.prepare(new FieldName("outlook"), lineArgs[3].isEmpty() ? "overcast" : lineArgs[3]);

        arguments.put(new FieldName("temperature"), temperature);
        arguments.put(new FieldName("humidity"), humidity);
        arguments.put(new FieldName("windy"), windy);
        arguments.put(new FieldName("outlook"), outlook);

        return arguments;
    }

    /**
     * Printing Active fields of Model
     * @param modelEvaluator
     */
    public void printArgumentsOfModel(ModelEvaluator<MiningModel> modelEvaluator){
        System.out.println("### Active Fields of Model ####");
        for(FieldName fieldName : modelEvaluator.getActiveFields()){
            System.out.println("Field Name: "+ fieldName);
        }
    }

    /**
     * Create PMML Object from .pmml file
     * @param fileName String
     * @return
     * @throws SAXException
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public PMML createPMMLfromFile(String fileName) throws SAXException, JAXBException, FileNotFoundException {
        File pmmlFile = new File(App.class.getResource(fileName).getPath());
        String pmmlString = new Scanner(pmmlFile).useDelimiter("\\Z").next();

        InputStream is = new ByteArrayInputStream(pmmlString.getBytes());

        InputSource source = new InputSource(is);
        SAXSource transformedSource = ImportFilter.apply(source);

        return JAXBUtil.unmarshalPMML(transformedSource);
    }
}
