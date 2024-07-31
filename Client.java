import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Client {
    private static final int TAMANHO_PACOTE = 1024;
    // private static final int TIMEOUT_MS = 10000;  // Timeout em milissegundos (10 segundos)

    public static void main(String[] args) {
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(); // Cria um socket UDP
            Scanner scanner = new Scanner(System.in); // Entrada do teclado

            // Solicita ao usuário que insira o endereço IP e porta do servidor
            System.out.print("Insira o endereço IP do servidor: ");
            String mensagem = scanner.nextLine();
            InetAddress servidorAddress = InetAddress.getByName(mensagem);
            System.out.print("Insira a porta do servidor: ");
            mensagem = scanner.nextLine();
            int portaServidor = Integer.parseInt(mensagem);

            // Solicita ao usuário que insira a mensagem de requisição
            System.out.print("Digite a mensagem a ser enviada: ");
            mensagem = scanner.nextLine();

            byte[] sendData = mensagem.getBytes(); // Converte a mensagem em bytes

            // Cria um pacote com os dados, o endereço e a porta do servidor
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, servidorAddress, portaServidor);

            // Define o timeout do socket
            //socket.setSoTimeout(TIMEOUT_MS);

            socket.send(sendPacket); // Envia o pacote

            // Se a mensagem for um pedido do tipo "GET /arquivo", aguarda a resposta do servidor
            if (mensagem.startsWith("GET /")) {
                // Guarda o nome do arquivo
                String arquivo = mensagem.substring(5);
                // Recebe os dados do servidor e os salva no arquivo
                byte[] receiveData = new byte[TAMANHO_PACOTE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                // Verifica se o timeout foi atingido
                /*
                try {
                    socket.receive(receivePacket);
                } catch (java.net.SocketTimeoutException timeoutException) {
                    System.out.println("Timeout atingido. Não foi possível receber a resposta do servidor.");
                    return;
                }*/

                socket.receive(receivePacket);

                // Verifica se a mensagem é um aviso de arquivo não encontrado
                String mensagemErro = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if (mensagemErro.startsWith("Arquivo não encontrado")) {
                    System.out.println(mensagemErro);
                } else {
                    // Solicita ao usuário o nome do arquivo para salvar
                    System.out.print("Digite o nome do arquivo para salvar: ");
                    String nomeArquivo = scanner.nextLine();

                    // Salva o CheckSum do servidor
                    socket.receive(receivePacket);
                    String mensagemCheck = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println(mensagemCheck);

                    // Cria um arquivo para salvar os dados recebidos
                    FileOutputStream fileOutputStream = new FileOutputStream(nomeArquivo);

                    // Recebe os dados restantes e salva no arquivo
                    while (true) {
                        socket.receive(receivePacket);
                        int bytesRead = receivePacket.getLength();
                        if (bytesRead == 0) {
                            break;  // Fim do arquivo
                        }
                        fileOutputStream.write(receivePacket.getData(), 0, bytesRead);
                    }

                    fileOutputStream.close();

                    // Verifica se o arquivo foi criado com sucesso
                    File arquivoCheck = new File(nomeArquivo);
                    if (arquivoCheck.exists()) {
                        // CheckSum do cliente
                        String checksum = "Checksum SHA-256 " + arquivo + ": " + calcularChecksumSHA256(arquivoCheck);
                        if (mensagemCheck.equals(checksum)) {
                            System.out.println("Arquivo com CheckSum iguais: " + nomeArquivo);
                        } else {
                            System.out.println("Arquivo com Checksum diferente:");
                        }
                        System.out.println(checksum + " (Cliente)");
                        System.out.println(mensagemCheck + " (Servidor)");
                    } else {
                        System.out.println("Falha na criação do arquivo...");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public static String calcularChecksumSHA256(File arquivo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(new FileInputStream(arquivo), md)) {
                while (dis.read() != -1) {
                    // lê o arquivo para calcular o hash
                }
            }

            byte[] hashBytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
