package io.github.jelilio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.github.jelilio.config.CompilerConfig;
import io.github.jelilio.model.Output;
import io.github.jelilio.model.Source;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/compiler")
@ApplicationScoped
public class CompilerSocket {
  private static final Logger logger = LoggerFactory.getLogger(CompilerSocket.class);

  Map<String, Session> sessions = new ConcurrentHashMap<>();


  @Inject
  CompilerConfig compilerConfig;

  @OnOpen
  public void onOpen(Session session) {
    logger.debug("onOpen: {}", session.getId());
    String id = session.getId();
    sessions.put(id, session);
  }

  @OnClose
  public void onClose(Session session) {
    logger.debug("onClose: {}", session.getId());
    String id = (sessions.remove(session.getId())).getId();
    deleteFiles(id); // delete all associated session files
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    logger.debug("onError: {}", session.getId());
    throwable.printStackTrace();
    String id = session.getId();
    sessions.remove(id);
    deleteFiles(id); // delete all associated session files
  }

  @OnMessage
  public void onMessage(String strData, Session session) {
    logger.debug("onMessage: {}", session.getId());
    String id = session.getId();

    compile(id, strData);
  }

  void deleteFiles(String sessionFilename) {
    logger.debug("deleteFiles: session: {}", sessionFilename);
    final File downloadDirectory = new File(compilerConfig.directory());
    final String[] langExtensions = compilerConfig.languageExt().values().toArray(new String[0]);
    final String[] extensions = Arrays.copyOf(langExtensions, langExtensions.length + 1);
    extensions[langExtensions.length] = compilerConfig.outputExt();

    FileUtils.listFiles(downloadDirectory, extensions, true)
        .stream().filter(file -> {
          var criteria = "%s.*?".formatted(sessionFilename);
          return file.getName().matches(criteria);
        }).forEach(File::delete);
  }

  void deleteFiles(String language, String sessionFilename) {
    logger.debug("deleteFiles: {}, {}", language, sessionFilename);
    final File downloadDirectory = new File(compilerConfig.directory(), language);
    final String languageExt = compilerConfig.languageExt().get(language);
    final String[] extensions = new String[]{languageExt, compilerConfig.outputExt()};

    FileUtils.listFiles(downloadDirectory, extensions, true)
        .stream().filter(file -> {
          var criteria = "%s.*?".formatted(sessionFilename);
          return file.getName().matches(criteria);
        }).forEach(File::delete);
  }

  @Transactional
  @Scheduled(cron = "{cron.expr}")
  void schedule() {
    logger.debug("scheduler running...");
    checkIfOutputExistAndNotEmpty();
  }


  void checkIfOutputExistAndNotEmpty() {
    final File directory = new File(compilerConfig.directory());
    final String[] extensions = new String[]{compilerConfig.outputExt()};

    Collection<String> sessionFilenames = sessions.keySet();
    sessionFilenames.forEach(sessionFilename -> {
      final Map<String, List<String>> outputCodes = new ConcurrentHashMap<>();

      Set<File> outputFiles = FileUtils.listFiles(directory, extensions, true)
          .stream().filter(file -> {
            var criteria = "%s.*?".formatted(sessionFilename);
            return file.getName().matches(criteria);
          }).collect(Collectors.toSet());

      for (File outputFile : outputFiles) {
        String language = outputFile.getParentFile().getName();
        if (outputFile.length() > 0) {
          // file not empty
          Path filename = Path.of(outputFile.getAbsolutePath());

          // Now calling Files.readString() method to
          // read the file
          try {
            List<String> output = Files.readAllLines(filename);

            deleteFiles(language, sessionFilename); // delete both the source and output file
            outputCodes.put(language, output);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

      if(!outputCodes.isEmpty()) {
        Output output = new Output(sessionFilename, outputCodes);
        Session session = sessions.get(sessionFilename);
        sendData(session, output);
      }
    });
  }

  private void sendData(Session session, Output output) {
    String extract = Json.encode(output);
    session.getAsyncRemote().sendObject(extract, result ->  {
      if (result.getException() != null) {
        logger.debug("Unable to send message: {}", result.getException().getMessage());
      } else {
        logger.debug("message sent");
      }
    });
  }

  private void compile(String id, String strData) {
    Source source = Json.decodeValue(strData, Source.class);

    if(source.files().isEmpty()) {
      return;
    }

    String language = source.language();
    String ext = compilerConfig.languageExt().get(language);
    String content = source.files().get(0).content(); // Only processing the first files

    try (Writer writer = new BufferedWriter(new OutputStreamWriter(
        new FileOutputStream("%s/%s/%s.%s".formatted(compilerConfig.directory(), language, id, ext)), StandardCharsets.UTF_8))) {
      writer.write(content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
