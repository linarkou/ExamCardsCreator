package examcardscreator;

import jade.core.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

public class ExamCardAgent extends Agent
{

    private Question firstQuestion;
    private Question secondQuestion;

    protected void setup()
    {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("card");
        sd.setName("MyCard");
        dfd.addServices(sd);
        try
        {
            DFService.register(this, dfd);
        } catch (FIPAException fe)
        {
            fe.printStackTrace();
        }
        addBehaviour(new QuestionRequester(this, 1000));
        addBehaviour(new Exchanger(this, 1000));
        System.out.println(this.getLocalName() + "создан");
    }

    private class QuestionRequester extends TickerBehaviour
    {
        boolean isSendMsgToManager = false;
        private HashSet<AID> questionAgents;

        public QuestionRequester(Agent a, long period)
        {
            super(a, period);
        }

        @Override
        public void onTick()
        {
            if (isSendMsgToManager)
                return;
            if (firstQuestion != null && secondQuestion != null && !isSendMsgToManager)
            {
                System.out.println(this.myAgent.getLocalName() + ": " + firstQuestion.toString() + " : " + secondQuestion.toString());
                AID manager = null;
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("manager");
                template.addServices(sd);
                try
                {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length != 0)
                    {
                        manager = result[0].getName();
                    } else
                    {
                        return;
                    }
                } catch (FIPAException fe)
                {
                    fe.printStackTrace();
                }
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.addReceiver(manager);
                message.setContent("" + (firstQuestion.complexity + secondQuestion.complexity));
                message.setReplyWith("ready" + System.currentTimeMillis());
                myAgent.send(message);
                block();
                isSendMsgToManager = true;
            }
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("question");
            template.addServices(sd);
            try
            {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                questionAgents = new HashSet<AID>();
                for (int i = 0; i < result.length; ++i)
                {
                    questionAgents.add(result[i].getName());
                }
            } catch (FIPAException fe)
            {
                fe.printStackTrace();
            }
            ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
            for (AID aid : questionAgents)
            {
                message.addReceiver(aid);
            }
            message.setContent("Дай себя");
            message.setReplyWith("request" + System.currentTimeMillis());
            myAgent.send(message);
            myAgent.addBehaviour(new QuestionPicker());
        }

        private class QuestionPicker extends Behaviour //принимает сообщения от вопросов, которые готовы "отдать" себя, и проверяет, можно ли взять вопрос в билет
        {

            int step = 0;
            ACLMessage msg;
            MessageTemplate mt;

            @Override
            public void action()
            {
                switch (step)
                {
                    case 0:
                        mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                        msg = myAgent.receive(mt);

                        if (msg != null)
                        {
                            if (firstQuestion == null)
                            {
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                reply.setContent("Беру");
                                myAgent.send(reply);
                                step = 1;
                            } else if (secondQuestion == null)
                            {
                                Question q = new Question(msg.getContent());
                                if (!firstQuestion.theme.equals(q.theme))
                                {
                                    ACLMessage reply = msg.createReply();
                                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    reply.setContent("Беру");
                                    myAgent.send(reply);
                                    step = 2;
                                }
                            }
                        } else
                        {
                            block();
                        }
                        break;
                    case 1:
                        mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                        msg = myAgent.receive(mt);

                        if (msg != null)
                        {
                            if (msg.getPerformative() == ACLMessage.AGREE)
                                if (firstQuestion == null)
                                {
                                    firstQuestion = new Question(msg.getContent());
                                    System.out.println("Билет "+myAgent.getLocalName() + " выбрал вопрос " + msg.getSender().getLocalName() + " - " + firstQuestion.toString());
                                    step = 0;
                                }
                            if (msg.getPerformative() == ACLMessage.REFUSE)
                            {
                                firstQuestion = null;
                                step = 0;
                            }
                        } else
                        {
                            block();
                        }
                        break;
                    case 2:
                        mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                        msg = myAgent.receive(mt);

                        if (msg != null)
                        {
                            if (msg.getPerformative() == ACLMessage.AGREE)
                                if (secondQuestion == null)
                                {
                                    secondQuestion = new Question(msg.getContent());
                                    System.out.println("Билет "+myAgent.getLocalName() + " выбрал вопрос " + msg.getSender().getLocalName() + " - " + secondQuestion.toString());
                                    step = 0;
                                }
                            if (msg.getPerformative() == ACLMessage.REFUSE)
                            {
                                secondQuestion = null;
                                step = 0;
                            }
                        } else
                        {
                            block();
                        }
                }
            }

            @Override
            public boolean done()
            {
                return (step == 1 && firstQuestion != null) || (step == 2 && secondQuestion != null);

            }
        }

        private class QuestionSetter extends CyclicBehaviour //запоминает вопросы
        {

            @Override
            public void action()
            {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null)
                {
                    if (firstQuestion == null)
                    {
                        firstQuestion = new Question(msg.getContent());
                    } else
                    {
                        Question q = new Question(msg.getContent());
                        if (!firstQuestion.theme.equals(q.theme))
                        {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            reply.setContent("Беру");
                            myAgent.send(reply);
                        }
                    }
                } else
                {
                    block();
                }
            }
        }
    }

    private class Exchanger extends TickerBehaviour
    {
        int step = 0;
        boolean isInitiator = false;
        MessageTemplate mt;
        ACLMessage msg;
        double average;
        boolean isChanged = false;
        int countOfNotAnsweredSimples = 0;

        public Exchanger(Agent a, long period)
        {
            super(a, period);
        }

        @Override
        protected void onTick()
        {
            //System.out.println(step);
            switch (step)
            {
                case 0:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM); //принимаем от менеджера сообщение о том, какая средняя сложность билета
                    msg = myAgent.receive(mt);

                    if (msg != null)
                    {
                        average = Double.parseDouble(msg.getContent());
                        if (firstQuestion.complexity + secondQuestion.complexity > average)
                        {
                            isInitiator = true;
                        }

                        try
                        {
                            DFService.deregister(this.myAgent);
                        } catch (FIPAException fe)
                        {
                            fe.printStackTrace();
                        }
                        //в зависимости от средней сложности меняем сервис билета - Initiator если >average, simple - если <average
                        DFAgentDescription dfd = new DFAgentDescription();
                        dfd.setName(getAID());
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType(isInitiator ? "initiator" : "simple");
                        sd.setName("MyCard");
                        dfd.addServices(sd);
                        try
                        {
                            DFService.register(this.myAgent, dfd);
                        } catch (FIPAException fe)
                        {
                            fe.printStackTrace();
                        }
                        
                        //отправляем менеджеру сообщение, что поменяли сервис
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent("Я поменял сервис");
                        myAgent.send(reply);
                        
                        step = 1;
                    } else
                    {
                        block();
                    }
                    break;
                case 1:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM); 
                    msg = myAgent.receive(mt);

                    if (msg != null)
                    {
                        if (msg.getContent().equals("Начинайте обмен!"))
                            step = 2;
                    }
                    else
                    {
                        block();
                    }
                    break;
                case 2:
                    if (isInitiator)
                    {
                        //отправляем всем simple-билетам свои вопросы
                        HashSet<AID> simpleCards = new HashSet<>();
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("simple");
                        template.addServices(sd);
                        try
                        {
                            DFAgentDescription[] result = DFService.search(myAgent, template);
                            countOfNotAnsweredSimples = result.length;
                            for (DFAgentDescription card : result)
                            {
                                simpleCards.add(card.getName());
                            }
                        } catch (FIPAException fe)
                        {
                            fe.printStackTrace();
                        }
                        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                        for (AID card : simpleCards)
                        {
                            message.addReceiver(card);
                        }
                        message.setContent("Дай свои вопросы");
                        message.setReplyWith("request" + System.currentTimeMillis());
                        myAgent.send(message);
                        step = 3;
                    }
                    else
                    {
                        msg = myAgent.receive();
                        if (msg != null)
                        {
                            if (msg.getPerformative() == ACLMessage.REQUEST && !isChanged)
                            {
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.PROPOSE);
                                reply.setContent(firstQuestion.toString() + ":" + secondQuestion.toString());
                                myAgent.send(reply);
                            }
                            else
                            {
                                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && !isChanged)
                                {
                                    isChanged = true;
                                    String[] split = msg.getContent().split(":");
                                    firstQuestion = new Question(split[0]);
                                    secondQuestion = new Question(split[1]);
                                    ACLMessage reply = msg.createReply();
                                    reply.setContent(split[2] + ":" + split[3]);
                                    reply.setPerformative(ACLMessage.AGREE);
                                    myAgent.send(reply);
                                }
                                else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && isChanged)
                                {
                                    ACLMessage reply = msg.createReply();
                                    reply.setPerformative(ACLMessage.REFUSE);
                                    reply.setContent("У моих вопросов уже нормальная сложность"); 
                                    myAgent.send(reply);
                                }
                            }
                        }
                        else
                        {
                            block();
                        }
                    }
                    break;
                case 3:
                    if (isInitiator && !isChanged)
                    {
                        mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                        msg = myAgent.receive(mt);
                        if (msg != null)
                        {
                            String[] split = msg.getContent().split(":");
                            if (split.length != 2)
                            {
                                int k =0;
                                k++;
                                System.out.println(msg.getSender().getLocalName());
                            }
                            Question q1 = new Question(split[0]);
                            Question q2 = new Question(split[1]);
                            Question[] qs = new Question[] {q1, q2, firstQuestion, secondQuestion};
                            if (check(qs[0],qs[2],qs[1],qs[3]))
                            {
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                reply.setContent(qs[0].toString() + ":" + qs[2].toString() + ":" + qs[1].toString() + ":" + qs[3].toString()); 
                                myAgent.send(reply);
                                step = 4;
                            } 
                            else
                            {
                                if (check(qs[0],qs[3],qs[1],qs[2]))
                                {
                                    ACLMessage reply = msg.createReply();
                                    reply.setPerformative(ACLMessage.CFP);
                                    reply.setContent(qs[1].toString() + ":" + qs[2].toString() + ":" + qs[0].toString() + ":" + qs[3].toString());
                                    myAgent.send(reply);
                                    step = 4;
                                }
                                else
                                {
                                    countOfNotAnsweredSimples--;
                                    if (countOfNotAnsweredSimples <= 0) //если нам ответили все simple-билеты
                                        step = 5;
                                }
                            }
                        }
                        else
                            block();
                    }
                    break;
                case 4:
                    if (isInitiator && !isChanged)
                    {
                        mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),MessageTemplate.MatchPerformative(ACLMessage.AGREE));
                        msg = myAgent.receive(mt);
                        if (msg != null)
                        {
                            countOfNotAnsweredSimples--;
                            if (msg.getPerformative() == ACLMessage.AGREE)
                            {
                                String[] split = msg.getContent().split(":");
                                firstQuestion = new Question(split[0]);
                                secondQuestion = new Question(split[1]);
                                isChanged = true;
                                step = 5;
                            }
                            if (msg.getPerformative() == ACLMessage.REFUSE)
                            {
                                step = 3; //при отказе в обмене вопросами снова ждем предложения о вопросах
                                if (countOfNotAnsweredSimples <= 0) //если нам ответили все simple-билеты
                                    step = 5;
                            }
                        }
                        else
                            block();
                    }
                    break;
                case 5:
                    AID manager = null;
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("manager");
                    template.addServices(sd);
                    try
                    {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result.length != 0)
                        {
                            manager = result[0].getName();
                        } else
                        {
                            return;
                        }
                    } catch (FIPAException fe)
                    {
                        fe.printStackTrace();
                    }
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.addReceiver(manager);
                    message.setContent("Обмен закончен");
                    message.setReplyWith("ready" + System.currentTimeMillis());
                    myAgent.send(message);
                    block();
                    step = 6;
                    break;
                    
            }
        }
        
        boolean check(Question q11, Question q12, Question q21, Question q22) {
            if (!q11.theme.equals(q12.theme) && !q21.theme.equals(q22.theme)) {
                if(q11.complexity+q12.complexity < average + average/10
                        && q11.complexity+q12.complexity > average - average/10
                        && q21.complexity+q22.complexity < average + average/10
                        && q21.complexity+q22.complexity > average - average/10)
                    return true;
            }
            return false;
        }
    }
        
}


