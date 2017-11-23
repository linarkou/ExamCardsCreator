package examcardscreator;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuestionAgent extends Agent {
    
    Question q;
    boolean isBusy = false;

    @Override
    protected void setup() {
        
        System.out.println("Question " + getLocalName()+ " is ready!");
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(QuestionAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Object[] args = getArguments();

        if (args != null && args.length == 3) {
            
            String theme = args[0].toString();
            String text = args[1].toString();       
            
            try {
                int complexity = Integer.parseInt(args[2].toString());
                q = new Question(theme, text, complexity);
            } catch (NumberFormatException ex) {
                this.takeDown();
            }
        } 
        else {
            /* Удаляем агента если не заданы параметры */
            this.takeDown();         
            return;
        }
        
        /* Регистрируем агента в системе */      
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("question");
        sd.setName("MyQuestion");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {

            @Override
            public void action()
            {
                ACLMessage msg = myAgent.receive();

                if (msg != null)
                {
                    if (msg.getPerformative() == ACLMessage.REQUEST && !isBusy)
                    {
                        ACLMessage reply = msg.createReply();
                        reply.setContent(q.toString());
                        reply.setPerformative(ACLMessage.PROPOSE);
                        myAgent.send(reply);
                        System.out.println(myAgent.getLocalName() + " ответил на запрос");
                    }
                    else
                    {
                        if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && !isBusy)
                        {
                            isBusy = true;
                            ACLMessage reply = msg.createReply();
                            reply.setContent(q.toString());
                            reply.setPerformative(ACLMessage.AGREE);
                            myAgent.send(reply);
                            System.out.println("Вопрос " + myAgent.getLocalName() + "(" + q.toString() + ")" + " выбран билетом " + msg.getSender().getLocalName());
                            /* Убираем вопрос из списка сервисов */
                            //myAgent.doDelete();
                            try {
                                DFService.deregister(this.myAgent);
                            } catch (FIPAException fe) {
                                fe.printStackTrace();
                            }
                        }
                        else
                        {
                            if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && isBusy)
                            {
                                ACLMessage reply = msg.createReply();
                                reply.setContent(q.toString());
                                reply.setPerformative(ACLMessage.REFUSE);
                                myAgent.send(reply);
                            }
                        }
                    }
                    if (msg.getPerformative() == ACLMessage.CANCEL)
                        {
                            isBusy = false;
                            System.err.println("CANCEL");
                            DFAgentDescription dfd = new DFAgentDescription();
                            dfd.setName(getAID());
                            ServiceDescription sd = new ServiceDescription();
                            sd.setType("question");
                            sd.setName("MyQuestion");
                            dfd.addServices(sd);
                            try {
                                DFService.register(this.myAgent, dfd);
                            } catch (FIPAException fe) {
                                fe.printStackTrace();
                            }
                            return;
                        }
                } else {
                    block();
                }
            }
        });
    }

    @Override
    public void takeDown()
    {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
