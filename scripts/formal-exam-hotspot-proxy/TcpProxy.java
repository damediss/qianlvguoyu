import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/** Debug-only TCP forwarder so a Pad on the Windows hotspot can reach Vite. */
public final class TcpProxy {
  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.err.println("usage: TcpProxy <bindHost> <listenPort> <destHost> <destPort>");
      System.exit(2);
    }
    String bindHost = args[0];
    int listenPort = Integer.parseInt(args[1]);
    String destHost = args[2];
    int destPort = Integer.parseInt(args[3]);
    try (ServerSocket server = new ServerSocket()) {
      server.setReuseAddress(true);
      server.bind(new InetSocketAddress(bindHost, listenPort));
      System.out.println("proxy " + bindHost + ":" + listenPort + " -> " + destHost + ":" + destPort);
      while (true) {
        Socket client = server.accept();
        Thread worker = new Thread(() -> handle(client, destHost, destPort), "hotspot-proxy");
        worker.setDaemon(true);
        worker.start();
      }
    }
  }

  private static void handle(Socket client, String destHost, int destPort) {
    try (Socket clientSocket = client;
        Socket dest = new Socket()) {
      dest.connect(new InetSocketAddress(destHost, destPort), 3000);
      dest.setTcpNoDelay(true);
      clientSocket.setTcpNoDelay(true);
      Thread upload = new Thread(() -> pipe(clientSocket, dest), "proxy-up");
      upload.setDaemon(true);
      upload.start();
      pipe(dest, clientSocket);
      upload.join(1000);
    } catch (Exception ignored) {
      // connection closed by either side
    }
  }

  private static void pipe(Socket inSocket, Socket outSocket) {
    try (InputStream in = inSocket.getInputStream();
        OutputStream out = outSocket.getOutputStream()) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) >= 0) {
        out.write(buffer, 0, read);
        out.flush();
      }
    } catch (Exception ignored) {
      try {
        outSocket.shutdownOutput();
      } catch (Exception ignoredClose) {
        // ignore
      }
    }
  }
}
