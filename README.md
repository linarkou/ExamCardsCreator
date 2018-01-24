# ExamCardsCreator
Multi-agent system for building a list of examination cards of approximately equal complexity.

Tickets are generated on the server, clients provide lists of questions.

The list of questions has format "\<Topic\>;\<Question Text\>;\<Difficulty\>" and specified in the file "questions.txt" on each client.

The required number of exam cards is set on the file "cards.txt" on server.

I used <a href="http://jade.tilab.com">JADE Framework</a>.
# How to run
<a href="server.bat">Server</a><br>
<a href="client.bat">Client</a> (-host section specifies server IP-address)
