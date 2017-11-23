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
        int rand = (new Random()).nextInt()%1000000;
        
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
}
