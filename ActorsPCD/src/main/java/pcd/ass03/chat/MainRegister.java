package pcd.ass03.chat;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import pcd.ass03.chat.actors.RegisterActor;

public class MainRegister {
	
	public static void main(final String[] args)  {
		final Config config = ConfigFactory.parseFile(new File("src/main/java/pcd/ass03/chat/register.conf"));
		final ActorSystem system = ActorSystem.create("ChatSystem", config);
		system.actorOf(RegisterActor.props(), "register");
	}
}
