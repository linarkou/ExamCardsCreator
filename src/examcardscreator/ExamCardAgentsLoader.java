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

public class ExamCardAgentsLoader extends Agent {
    @Override
    protected void setup() {
        int countOfExamCardAgents = 0;
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("cards.txt"), "utf-8"))) {  // или cp1251
            String currentLine;
            if ((currentLine = reader.readLine()) != null) 
            {
                countOfExamCardAgents = Integer.parseInt(currentLine.trim());
            }
        } catch (UnsupportedEncodingException ex)
        {
            Logger.getLogger(ExamCardAgentsLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(ExamCardAgentsLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        //ExamCardAgent creation
        int rand = (new Random()).nextInt();
        
        for (int i = 1; i <= countOfExamCardAgents; ++i)
        {
            try
            {
                AgentController ac = getContainerController().createNewAgent("b" + (rand+i), "examcardscreator.ExamCardAgent", null);
                ac.start();

            } catch (StaleProxyException ex)
            {
                Logger.getLogger(ExamCardAgentsLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Manager creation
        try
        {
            AgentController ac = getContainerController().createNewAgent("m1", "examcardscreator.Manager", null);
            ac.start();
        } catch (StaleProxyException ex)
        {
            Logger.getLogger(ExamCardAgentsLoader.class.getName()).log(Level.SEVERE, null, ex);
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
