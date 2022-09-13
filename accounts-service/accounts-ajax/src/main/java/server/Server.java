package server;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.jooby.Jooby;
import org.jooby.Results;

public class Server extends Jooby {

	public Server() {
		get("/favicon.ico", () -> Results.noContent());
		assets("/**");
		assets("/", "index.html");
	}

	public static void main(String[] args) throws IOException {
		Server server = new Server();

		server.port(7081);

		CompletableFuture.runAsync(() -> {
			server.start();
		});

		server.onStarted(() -> {
			System.out.println("\nPress Enter to stop service.");
		});

		System.in.read();
		System.exit(0);
	}

}
