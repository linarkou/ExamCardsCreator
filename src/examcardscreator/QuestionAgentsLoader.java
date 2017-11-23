package examcardscreator;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuestionAgentsLoader extends Agent {
    @Override
    protected void setup() {
        // QuestionAgent creation
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("questions.txt"), "utf-8"))){
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                lineCount++;
                AgentController ac = parseAgent(currentLine);
                if (ac != null) {
                    ac.start();
                }
            }
        } catch (IOException ex) {
            System.out.println("Reading error in line " + lineCount);
        } catch (StaleProxyException ex) {
            Logger.getLogger(QuestionAgentsLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private AgentController parseAgent(String s) throws StaleProxyException {
        String[] splitted = s.split(";");
        
        switch (splitted[0]) {
            case "q":               
                String agentName = splitted[0] + splitted[1];
                
                String theme = splitted[2];
                String text = splitted[3];               
                int complexity = Integer.parseInt(splitted[4]);

                Object[] args = new Object[] {theme, text, complexity};
                
                return getContainerController().createNewAgent(agentName, "examcardscreator.QuestionAgent", args);
                
            default:
                return null;
        }        
    }
}
