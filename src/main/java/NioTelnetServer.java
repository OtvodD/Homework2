import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioTelnetServer {
    private static final String LS_COMMAND = "\tls     view all files from current directory";
    private static final String MKDIR_COMMAND = "\tmkdir  view all files from current directory";
    private static final String root = "server";

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private Map<SocketAddress, String> clients = new HashMap<>();

    public NioTelnetServer() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5679));
        server.configureBlocking(false);
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "skjghksdhg");
        channel.write(ByteBuffer.wrap("Hello user!\n".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info".getBytes(StandardCharsets.UTF_8)));
    }


    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);

        if (readBytes < 0) {
            channel.close();
            return;
        } else  if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        // TODO: 21.06.2021
        // touch (filename) - создание файла
        // mkdir (dirname) - создание директории
        // cd (path | ~ | ..) - изменение текущего положения
        // rm (filename / dirname) - удаление файла / директории
        // copy (src) (target) - копирование файлов / директории
        // cat (filename) - вывод содержимого текстового файла
        // changenick (nickname) - изменение имени пользователя

        // добавить имя клиента

        if (key.isValid()) {
            String [] command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "")
                    .split(" ");
            if ("--help".equals(command[0])) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
            } else if ("ls".equals(command[0])) {
                sendMessage(getFilesList().concat("\n"), selector, client);
            } else if ("touch".equals(command[0])){
                createFile(command[1]);
            } else if ("mkdir".equals(command[0])){
                createDir(command[1]);
            } else if ("cd".equals(command[0])){
//                transitionDir(command[1]);
            } else if ("rm".equals(command[0])){
                removal(command[1]);
            } else if ("copy".equals(command[0])){
                coppy(command[1]);
            } else if ("cat".equals(command[0])){
//                cat(command[1]);
            }
        }
    }

//    private void cat(String name) throws IOException {
//        BufferedReader br = new BufferedReader(new FileReader(name));
//        String line = null;
//        while ((line = br.readLine()) != null){
//            System.out.println(line);
//        }
//    }

    // у меня не получилось реализоать имя пользователя и преход по дирректориям
    // У меня на этой неделе заканчивается практика в магстратуре и я найду время чтобы в этом разобраться

    private void coppy(String name) throws IOException {
        Path pathSource = Paths.get(root + File.separator + name);
        Path pathTarget = Paths.get(root + File.separator);
        Files.copy(pathSource, pathTarget, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void removal(String name) throws IOException {
        Path path = Paths.get(root + File.separator + name);
        Files.delete(path);
    }

//    private void transitionDir(String dirname) {
//        Path path = Paths.get(root);
//        if (dirname.equals("..")){
//            path = Paths.get(root);
//        } else if (dirname.equals("~")){
//            path = Paths.get()
//        }
//    }

    private void createDir(String dirname) throws IOException {
        String root = "Server";
        Files.createDirectories(Paths.get(root + File.separator + dirname));
    }

    private void createFile(String filename) throws IOException {
        Path path = Paths.get(root + File.separator + filename);

        if(Files.exists(path)){
            System.out.println("File almost exist");
        }
        else if(!Files.exists(path)){
            Files.createFile(path);
        }

    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private String getFilesList() {
        String[] servers = new File("server").list();
        return String.join(" ", servers);
    }

    public static void main(String[] args) throws Exception {
        new NioTelnetServer();
    }
}