# ExamCardsCreator
Мультиагентная система для построения списка билетов примерно равной сложности из списка вопросов.

Список вопросов задается в файле questions.txt в формате: "<Тема>;<Текст вопроса>;<Сложность>".

Необходимое количество билетов задается в файле cards.txt.

Билеты формируются на сервере, клиенты предоставляют списки вопросов.

Для запуска сервера использовать server.bat

Для запуска клиента использовать client.bat, предварительно изменив в нем -host <IP> на IP-адрес сервера.

