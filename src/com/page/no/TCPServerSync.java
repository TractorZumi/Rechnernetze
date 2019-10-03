/*
 * TCPServer.java
 *
 * Version 3.1
 * Autor: M. Hübner HAW Hamburg (nach Kurose/Ross)
 * Zweck: TCP-Server Beispielcode:
 *        Bei Dienstanfrage einen Arbeitsthread erzeugen, der eine Anfrage bearbeitet:
 *        einen String empfangen, in Großbuchstaben konvertieren und zurücksenden
 *        Maximale Anzahl Worker-Threads begrenzt durch Java-Synchronisation (wait/notify)
 */
import java.io.*;

import java.net.*;


public class TCPServerSync {
   /* TCP-Server, der Verbindungsanfragen entgegennimmt */

   /* Portnummer */
   private final int serverPort;

   /* Maximal erlaubte Anzahl paralleler worker threads */
   private final int maxThreads;

   /* Counts the number of parallel running worker threads  */
   private int runningThreads = 0;

   /* Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler Worker-Threads*/
   public TCPServerSync(int serverPort, int maxThreads) {
      this.serverPort = serverPort;
      this.maxThreads = maxThreads;
   }

   public void startServer() {
      ServerSocket welcomeSocket; // TCP-Server-Socketklasse
      Socket connectionSocket; // TCP-Standard-Socketklasse

      int nextThreadNumber = 0;

      try {
         /* Server-Socket erzeugen */
         welcomeSocket = new ServerSocket(serverPort);

         while (true) { // Server laufen IMMER
            System.out.println("TCP Server is waiting for connection - listening TCP port " +
                  serverPort);
            /*
             * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
             * Standard-Socket erzeugen und an connectionSocket zuweisen
             */
            connectionSocket = welcomeSocket.accept();

            /* Neuen Arbeits-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt übergeben */
            (new TCPWorkerThread(++nextThreadNumber, connectionSocket, this)).start();
            incRunningThreads();
         }
      } catch (IOException e) {
         System.err.println(e.toString());
      }
   }

   public synchronized void incRunningThreads() {
      /* Increase the number of parallel running worker threads. If maximum reached: wait */
      runningThreads++;

      if (runningThreads >= maxThreads) {
         try {
            System.out.println(maxThreads +
                  " Threads running --> new Connections will not be handled, but queued ...");
            this.wait();
         } catch (InterruptedException e) {
            System.out.println("INTERRUPTED!");
         }
      }
   }

   public synchronized void decRunningThreads() {
      /* Decrease the number of parallel running worker threads and notify (awake) server */
      runningThreads--;
      this.notify();
   }

   public static void main(String[] args) {
      /* Erzeuge Server und starte ihn */
      TCPServerSync myServer = new TCPServerSync(56789, 3);
      myServer.startServer();
   }
}


// ----------------------------------------------------------------------------
class TCPWorkerThread extends Thread {
   /*
    * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
    * erhält
    */
   private int name;
   private Socket socket;
   private TCPServerSync server;
   private BufferedReader inFromClient;
   private DataOutputStream outToClient;
   boolean serviceRequested = true; // Arbeitsthread beenden?

   public TCPWorkerThread(int num, Socket sock, TCPServerSync server) {
      /* Konstruktor */
      this.name = num;
      this.socket = sock;
      this.server = server;
   }

   public void run() {
      String capitalizedSentence;

      System.out.println("TCP Worker Thread " + name +
            " is running until QUIT is received!");

      try {
         /* Socket-Basisstreams durch spezielle Streams filtern */
         inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         outToClient = new DataOutputStream(socket.getOutputStream());

         while (serviceRequested) {
            /* String vom Client empfangen und in Großbuchstaben umwandeln */
            capitalizedSentence = readFromClient().toUpperCase();

            /* Modifizierten String an Client senden */
            writeToClient(capitalizedSentence);

            /* Test, ob Arbeitsthread beendet werden soll */
            if (capitalizedSentence.indexOf("QUIT") > -1) {
               serviceRequested = false;
            }
         }

         /* Socket-Streams schließen --> Verbindungsabbau */
         socket.close();
      } catch (IOException e) {
         System.err.println("Connection aborted by client!");
      } finally {
			server.decRunningThreads();
         System.out.println("TCP Worker Thread " + name + " stopped!");
      }
   }

   private String readFromClient() throws IOException {
      /* Lies die nächste Anfrage-Zeile (request) vom Client */
      String request = inFromClient.readLine();
      System.out.println("TCP Worker Thread " + name + " detected job: " + request);

      return request;
   }

   private void writeToClient(String reply) throws IOException {
      /* Sende den String als Antwortzeile (mit newline) zum Client */
      outToClient.writeBytes(reply + '\r' + '\n');
      System.out.println("TCP Worker Thread " + name +
            " has written the message: " + reply);
   }
}
