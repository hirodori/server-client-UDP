import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Server {
    private static final int TAMANHO_PACOTE = 1024;

    public static void main(String[] args) {
        DatagramSocket socket = null;

        try {
            // Cria um socket UDP na porta 7777
            socket = new DatagramSocket(7777);
            System.out.println("Servidor UDP iniciado. Aguardando mensagens...");

            while (true) {
                byte[] receiveData = new byte[TAMANHO_PACOTE];

                // Cria um pacote para receber os dados
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                // Aguarda a chegada de um pacote
                socket.receive(receivePacket);

                // Extrai os dados do pacote recebido
                String mensagem = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // Obtém informações sobre o cliente
                String enderecoCliente = receivePacket.getAddress().getHostAddress();
                int portaCliente = receivePacket.getPort();

                System.out.println("Mensagem recebida de " + enderecoCliente + ":" + portaCliente + ": " + mensagem);

                // Processa a mensagem e envia o arquivo, se necessário
                processarMensagem(mensagem, enderecoCliente, portaCliente, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private static void processarMensagem(String mensagem, String enderecoCliente, int portaCliente, DatagramSocket socket) {
        try {
            if (mensagem.startsWith("GET /")) {
                // Guarda o nome do arquivo da mensagem
                String nomeArquivo = mensagem.substring(5);

                // Verifica se o arquivo existe
                File arquivo = new File(nomeArquivo);
                if (arquivo.exists()) {
                    String mensagemExistente = "Arquivo existente: " + nomeArquivo;
                    DatagramPacket sendPacket = new DatagramPacket(mensagemExistente.getBytes(), mensagemExistente.length(), InetAddress.getByName(enderecoCliente), portaCliente);
                    socket.send(sendPacket);
                    System.out.println("Mensagem sobre existência de arquivo enviada para " + enderecoCliente + ":" + portaCliente);
                    enviarArquivo(arquivo, enderecoCliente, portaCliente, socket);
                } else {
                    // Envia mensagem informando que o arquivo não existe
                    String mensagemErro = "Arquivo não encontrado: " + nomeArquivo;
                    DatagramPacket sendPacket = new DatagramPacket(mensagemErro.getBytes(), mensagemErro.length(), InetAddress.getByName(enderecoCliente), portaCliente);
                    socket.send(sendPacket);
                    System.out.println("Mensagem de erro enviada para " + enderecoCliente + ":" + portaCliente);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enviarArquivo(File arquivo, String enderecoCliente, int portaCliente, DatagramSocket socket) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(arquivo))) {
            byte[] buffer = new byte[TAMANHO_PACOTE];
            int bytesRead;

            // Envia um pacote com o CheckSum do arquivo
            String checksum = "Checksum SHA-256 " + arquivo + ": " + calcularChecksumSHA256(arquivo);
            System.out.println(checksum);
            DatagramPacket checkPacket = new DatagramPacket(checksum.getBytes(), checksum.length(), InetAddress.getByName(enderecoCliente), portaCliente);
            socket.send(checkPacket);

            // Envio do arquivo em si
            while ((bytesRead = bis.read(buffer)) != -1) {
                DatagramPacket sendPacket = new DatagramPacket(buffer, bytesRead, InetAddress.getByName(enderecoCliente), portaCliente);
                socket.send(sendPacket);
                Thread.sleep(10);  // Atraso para evitar congestionamento
            }

            // Envia um pacote vazio indicando o final do arquivo
            DatagramPacket finalPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(enderecoCliente), portaCliente);
            socket.send(finalPacket);

            System.out.println("Arquivo enviado para " + enderecoCliente + ":" + portaCliente);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
