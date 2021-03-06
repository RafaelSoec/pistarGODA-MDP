package br.unb.cic.goda.rtgoretoprism.paramwrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.unb.cic.goda.rtgoretoprism.generator.CodeGenerationException;

/**
 * Façade to a PARAM executable.
 *
 * @author Thiago
 * <p>
 * Adapted to GODA by Gabriela
 */
public class ParamWrapper implements ParametricModelChecker {

    private static final Logger LOGGER = Logger.getLogger(ParamWrapper.class.getName());

    private String paramPath;
    private String prismPath;
    private String fileName;
    private boolean usePrism = false;

    public ParamWrapper(String prismParamPath, String fileName) {
        this.paramPath = prismParamPath + "/param";
        this.prismPath = prismParamPath + "/prism";
        this.fileName = fileName;
    }

    @Override
    public String getFormula(String model) throws CodeGenerationException {
        String reliabilityProperty = "P=? [ true U (s" + fileName + " = 2) ]";
        return evaluate(model, reliabilityProperty);
    }

    private String evaluate(String model, String property) {
        try {
            File modelFile = File.createTempFile("model", "param");
            FileWriter modelWriter = new FileWriter(modelFile);
            modelWriter.write(model);
            modelWriter.flush();
            modelWriter.close();

            File propertyFile = File.createTempFile("property", "prop");
            FileWriter propertyWriter = new FileWriter(propertyFile);
            propertyWriter.write(property);
            propertyWriter.flush();
            propertyWriter.close();

            File resultsFile = File.createTempFile("result", null);

            String formula;
            if (usePrism && !model.contains("param")) {
                formula = invokeModelChecker(modelFile.getAbsolutePath(),
                        propertyFile.getAbsolutePath(),
                        resultsFile.getAbsolutePath());
            } else {
                formula = invokeParametricModelChecker(modelFile.getAbsolutePath(),
                        propertyFile.getAbsolutePath(),
                        resultsFile.getAbsolutePath());
            }
            return formula.trim().replaceAll("\\s+", "");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        return "";
    }

    private String invokeParametricModelChecker(String modelPath,
                                                String propertyPath,
                                                String resultsPath) throws IOException {
        String commandLine = paramPath + " "
                + modelPath + " "
                + propertyPath + " "
                + "--result-file " + resultsPath;
        return invokeAndGetResult(commandLine, resultsPath + ".out");
    }

    private String invokeModelChecker(String modelPath,
                                      String propertyPath,
                                      String resultsPath) throws IOException {
        String commandLine = prismPath + " "
                + modelPath + " "
                + propertyPath + " "
                + "-exportresults " + resultsPath;
        return invokeAndGetResult(commandLine, resultsPath);
    }

    private String invokeAndGetResult(String commandLine, String resultsPath) throws IOException {
        LOGGER.fine(commandLine);
        Process program = Runtime.getRuntime().exec(commandLine);
        int exitCode = 0;
        try {
            exitCode = program.waitFor();
        } catch (InterruptedException e) {
        	LOGGER.severe("Error invoking param with command:" + commandLine);
        	
            LOGGER.severe("Exit code: " + exitCode);
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        
        logExecResults(program);
        
        List<String> lines = Files.readAllLines(Paths.get(resultsPath), Charset.forName("UTF-8"));
        // Formula
        return lines.get(lines.size() - 1);
    }
    
    private void logExecResults(Process proc) throws IOException {
        BufferedReader stdInput = new BufferedReader(new 
       	     InputStreamReader(proc.getInputStream()));
       
       BufferedReader stdError = new BufferedReader(new 
       	     InputStreamReader(proc.getErrorStream()));
       
       String s = null;
       
       if(stdInput.ready()) {
	       // Read the output from the command
	       System.out.println("Here is the standard output of the command:");
	       while ((s = stdInput.readLine()) != null) {
	           System.out.println(s);
	       }
       }

       if(stdError.ready()) {
           // Read any errors from the attempted command
           System.err.println("Here is the standard error of the command (if any):");
           while ((s = stdError.readLine()) != null) {
               System.err.println(s);
           }
       }
    }

}
