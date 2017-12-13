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

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

public class ExamCardAgent extends Agent
{
    static String filename = "output.txt";
    private Question firstQuestion;
    private Question secondQuestion;
    public double average = 0;
    boolean isChanged = false;
    
    HashSet<AID> simpleCards;
    boolean isPrinted = false;
    
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
        addBehaviour(new QuestionRequester(this, 3000));
        addBehaviour(new Exchanger(this, 3000));
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
            {
                return;
            }
            if (firstQuestion != null && secondQuestion != null && !isSendMsgToManager)
            {
                
                System.out.println("Билет готов - " + this.myAgent.getLocalName() + ": " + firstQuestion.toString() + " : " + secondQuestion.toString());
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
                return;
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
            //System.out.println(myAgent.getLocalName() + " запросил вопросы");
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
                                //System.out.println("Билет " + myAgent.getLocalName() + " хочет взять первый вопрос ");// + msg.getSender().getLocalName() + " - " + firstQuestion.toString());
                                
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
                                    //System.out.println("Билет " + myAgent.getLocalName() + " хочет взять второй вопрос " + msg.getSender().getLocalName() + " - " + firstQuestion.toString());
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
                            {
                                if (firstQuestion == null)
                                {
                                    firstQuestion = new Question(msg.getContent());
                                    System.out.println("Билет " + myAgent.getLocalName() + " выбрал вопрос " + msg.getSender().getLocalName() + " - " + firstQuestion.toString());
                                    step = 0;
                                }
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
                            {
                                if (secondQuestion == null && !firstQuestion.theme.equals((new Question(msg.getContent())).theme))
                                {
                                    secondQuestion = new Question(msg.getContent());
                                    System.out.println("Билет " + myAgent.getLocalName() + " выбрал вопрос " + msg.getSender().getLocalName() + " - " + secondQuestion.toString());
                                    step = 0;
                                }
                                else
                                {
                                    secondQuestion = null;
                                    step = 0;
                                    ACLMessage reply = msg.createReply();
                                    reply.setPerformative(ACLMessage.CANCEL);
                                    reply.setContent("Отмена");
                                    myAgent.send(reply);
                                }
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
    }
    
    private class Exchanger extends TickerBehaviour
    {
        
        int step;
        boolean isInitiator = false;
        MessageTemplate mt;
        ACLMessage msg;
        boolean isChanged = false;
        int countOfNotAnsweredSimples = 0;
        
        public Exchanger(Agent a, long period)
        {
            super(a, period);
            step = 0;
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
                        {
                            if (isInitiator)
                            {
                                myAgent.addBehaviour(new InitiatorRequester(this.myAgent, 1000));
                            } else
                            {
                                myAgent.addBehaviour(new SimpleBehaviour());
                            }
                            ((ExamCardAgent) myAgent).average = average;
                            //step = 2;
                        }
                    } else
                    {
                        block();
                    }
                    break;
            }
        }
        
        /*boolean check(Question q11, Question q12, Question q21, Question q22)
        {
            if (!q11.theme.equals(q12.theme) && !q21.theme.equals(q22.theme))
            {
                if (q11.complexity + q12.complexity < average + average / 50
                        && q11.complexity + q12.complexity > average - average / 50
                        && q21.complexity + q22.complexity < average + average / 50
                        && q21.complexity + q22.complexity > average - average / 50)
                {
                    return true;
                }
            }
            return false;
        }
        */
    }
    
    private class InitiatorRequester extends TickerBehaviour
    {
        int step = 0;
        
        public InitiatorRequester(Agent a, long period)
        {
            super(a, period);
            simpleCards = new HashSet<>();
        }
        
        @Override
        protected void onTick()
        {
            //if (isChanged)
                //return;
            switch (step)
            {
                case 0:
                    //отправляем всем simple-билетам свои вопросы
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("simple");
                    template.addServices(sd);
                    try
                    {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        //countOfNotAnsweredSimples = result.length;
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
                    message.setLanguage("1");
                    myAgent.send(message);
                    myAgent.addBehaviour(new InitiatorBehaviour());
            }
        }

    }
    
    private class InitiatorBehaviour extends Behaviour
    {

        int step = 1;
        ACLMessage msg;
        MessageTemplate mt;
        MessageTemplate mtl = MessageTemplate.MatchLanguage("1");
        
        @Override
        public void action()
        {
            if (isPrinted)
                return;
            if (simpleCards.size() <= 0 &&!isPrinted) 
                step=3;
            switch (step)
            {
                case 1:
                    //if (!isChanged)
                    //{
                    mt = MessageTemplate.and(mtl,MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
                    msg = myAgent.receive(mt);
                    if (msg != null)
                    {
                        System.out.println(myAgent.getLocalName() + " получил вопросы от " + msg.getSender().getLocalName());
                        String[] split = msg.getContent().split(":");
                        if (split.length != 2)
                        {
                            int k = 0;
                            k++;
                            System.err.println(msg.getSender().getLocalName());
                        }
                        Question q1 = new Question(split[0]);
                        Question q2 = new Question(split[1]);
                        Question[] qs = new Question[]
                        {
                            q1, q2, firstQuestion, secondQuestion
                        };
                        int sum1 = qs[0].complexity + qs[1].complexity;
                        int sum2 = qs[2].complexity + qs[3].complexity;
                        int rez0 = Math.abs(sum1-sum2);
                        int rez1 = check(qs[0], qs[2], qs[1], qs[3]);
                        int rez2 = check(qs[1], qs[2], qs[0], qs[3]);
                        if (rez0 > rez1 || rez0 > rez2) { //есть хорошие варианты обмена
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            if (rez1 < rez2)
                                reply.setContent(qs[0].toString() + ":" + qs[2].toString() + ":" + qs[1].toString() + ":" + qs[3].toString());
                            else 
                                reply.setContent(qs[1].toString() + ":" + qs[2].toString() + ":" + qs[0].toString() + ":" + qs[3].toString());
                            reply.setLanguage("1");
                            myAgent.send(reply);
                            step = 2;
                            System.out.println(myAgent.getLocalName() + " предложил поменяться " + msg.getSender().getLocalName());
                        } 
                        else
                        {
                            System.out.println("У " + myAgent.getLocalName() + " нет хороших вариантов обмена с " + msg.getSender().getLocalName());
                            simpleCards.remove(msg.getSender());
                            if (simpleCards.size() <= 0) //если нам ответили все simple-билеты
                            {
                                step = 3;
                            }
                        }
                    
                    } else
                    {
                        block();
                    }
                    //}
                    break;
                case 2:
                    mt = MessageTemplate.and(mtl,MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE), MessageTemplate.MatchPerformative(ACLMessage.AGREE)));
                    msg = myAgent.receive(mt);
                    if (msg != null)
                    {
                        //simpleCards.remove(msg.getSender());
                        if (msg.getPerformative() == ACLMessage.AGREE)
                        {
                            System.out.println(msg.getContent());
                            String[] split = msg.getContent().split(":");
                            firstQuestion = new Question(split[0]);
                            secondQuestion = new Question(split[1]);
                            System.out.println(myAgent.getLocalName() + " поменялся с " + msg.getSender().getLocalName());
                        }
                        if (msg.getPerformative() == ACLMessage.REFUSE)
                        {
                            System.out.println(myAgent.getLocalName() + " отказал в обмене " + msg.getSender().getLocalName());
                            simpleCards.remove(msg.getSender());
                            //step = 1; //при отказе в обмене вопросами снова ждем предложения о вопросах
                        }
                        step = 1;
                        //похоже условие ниже никогда не выполняется
                        if (simpleCards.size() <= 0) //если нам ответили все simple-билеты 
                            step = 3;
                    } else
                    {
                        block();
                    }
                    break;
                case 3:
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
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM_REF);
                    message.addReceiver(manager);
                    int sum = firstQuestion.complexity + secondQuestion.complexity;
                    message.setContent("Билет " + myAgent.getLocalName() + " готов: " + ((ExamCardAgent)myAgent).firstQuestion.toString() + ", " + ((ExamCardAgent)myAgent).secondQuestion.toString() + " Сложность=" + sum);
                    message.setReplyWith("ready" + System.currentTimeMillis());
                    myAgent.send(message);
                    //block();
                    step = 4;
                    break;
                case 4:
                    mt = MessageTemplate.and(MessageTemplate.MatchLanguage("1"), MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE));
                    msg = myAgent.receive(mt);
                    if (msg != null && !isPrinted) 
                    {
                        sum = firstQuestion.complexity + secondQuestion.complexity;
                        System.out.println("Билет " + myAgent.getLocalName() + " готов: " + ((ExamCardAgent)myAgent).firstQuestion.toString() + ", " + ((ExamCardAgent)myAgent).secondQuestion.toString() + " Сложность=" + sum);
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROXY);
                        reply.setContent("Билет " + myAgent.getLocalName() + " готов: " + ((ExamCardAgent)myAgent).firstQuestion.toString() + ", " + ((ExamCardAgent)myAgent).secondQuestion.toString() + " Сложность=" + sum);
                        reply.setReplyWith("ready" + System.currentTimeMillis());
                        myAgent.send(reply);
                        isPrinted = true;
                        step = 5;
                    }
                    break;
                    
            }
        }
        
        @Override
        public boolean done()
        {
            if (step == 5)
            {
                return true;
            }
            return false;
        }
        
        int check(Question q11, Question q12, Question q21, Question q22) //возвращает разницу сумм сложностей для каждого билета
        {
            if (!q11.theme.equals(q12.theme) && !q21.theme.equals(q22.theme))
            {
                int sum11 = q11.complexity + q12.complexity;
                int sum22 = q21.complexity + q22.complexity;
                {
                    return Math.abs(sum11-sum22);
                }
            }
            return Integer.MAX_VALUE;
        }
        
    }
    
    private class SimpleBehaviour extends CyclicBehaviour
    {
        
        ACLMessage msg;
        boolean isChanged = false;
        
        @Override
        public void action()
        {
            msg = myAgent.receive(MessageTemplate.MatchLanguage("1"));
            if (msg != null)
            {
                
                if (msg.getPerformative() == ACLMessage.REQUEST)
                {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(firstQuestion.toString() + ":" + secondQuestion.toString());
                    myAgent.send(reply);
                    //System.out.println(myAgent.getLocalName() + " отправил свои вопросы " + msg.getSender().getLocalName());
                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && !isChanged)
                {
                    isChanged = true;
                    String[] split = msg.getContent().split(":");
                    firstQuestion = new Question(split[0]);
                    secondQuestion = new Question(split[1]);
                    ACLMessage reply = msg.createReply();
                    reply.setContent(split[2] + ":" + split[3]);
                    reply.setPerformative(ACLMessage.AGREE);
                    reply.setLanguage("1");
                    myAgent.send(reply);
                    System.out.println(myAgent.getLocalName() + " согласен меняться с " + msg.getSender().getLocalName());
                    
                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && isChanged)
                {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("У моих вопросов уже нормальная сложность");
                    reply.setLanguage("1");
                    myAgent.send(reply);
                    System.out.println(myAgent.getLocalName() + " НЕ согласен меняться с " + msg.getSender().getLocalName());
                    
                }
                if (msg.getPerformative() == ACLMessage.PROPAGATE)
                {
                    int sum = firstQuestion.complexity + secondQuestion.complexity;
                    System.out.println("Билет " + myAgent.getLocalName() + " готов: " + ((ExamCardAgent)myAgent).firstQuestion.toString() + ", " + ((ExamCardAgent)myAgent).secondQuestion.toString() + " Сложность=" + sum);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROXY);
                    reply.setContent("Билет " + myAgent.getLocalName() + " готов: " + ((ExamCardAgent)myAgent).firstQuestion.toString() + ", " + ((ExamCardAgent)myAgent).secondQuestion.toString() + " Сложность=" + sum);
                    reply.setReplyWith("ready" + System.currentTimeMillis());
                    myAgent.send(reply);
                    //block();
                }
            } else
            {
                block();
            }
        }
        
    }
}
