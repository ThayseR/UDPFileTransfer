# UDPFileTransfer
Transfere arquivos via UDP simulando funcionamento do TCP com stop-and-wait. 
Código feito usando este site como referência: http://codereview.stackexchange.com/questions/23088/java-multithreaded-file-server-and-client-emulate-tcp-over-udp
O código é intuitivo, está transferindo cada pacote com 1024 bytes e com 2 segundos de timeout.
O Servidor ouve a porta 8550 e após receber uma conexao cria uma thread em ClientConnection para gerenciar recebimento ou envio de arquivos.
Ao executar o cliente é possível usar os comandos get e put para receber ou enviar arquivos.
