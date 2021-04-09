package commands;

import carver.ModularCarver;
import energy.BackwardsEnergy;
import energy.ForwardsEnergy;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import pathfinder.DefaultPathfinder;
import pathfinder.ForwardsPathfinder;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class CarveCommand extends Command {
    private ModularCarver[] carvers;

    // TODO: change modularcarver to accept path as a method rather than a constructor
    public CarveCommand() {
        carvers = new ModularCarver[2];
        //carvers[0] = new
    }

    public void execute(MessageChannel channel, User author, Message message) {
        if (message.getAttachments().isEmpty()) {
            sendSadSmoh(channel, "smoh.... (please send the image as an attachment!)");
            return;
        }

        String content = message.getContentRaw();
        String[] args = content.split(" ");
        Message.Attachment attachment = message.getAttachments().get(0);

        if (!attachment.isImage()) {
            sendSadSmoh(channel, "smoh.... (I don't recognize this file format..)");
            return;
        }

        try {
            String path = "src/main/resources/images/download.png"; //TODO: Refactor string paths using globs
            attachment.downloadToFile(path).get();

            channel.sendMessage("SMOH!!! (begins chopping)") //TODO: use embed builders
                    .addFile(new File("src/main/resources/assets/small_chop.gif")).queue();
            channel.sendTyping().queue();

            ModularCarver carver = null;
            switch (args[0]) {
                case "!carve" -> carver = new ModularCarver(path, new BackwardsEnergy(), new DefaultPathfinder());
                case "!fcarve" -> carver = new ModularCarver(path, new ForwardsEnergy(), new ForwardsPathfinder());
                default -> {
                    sendSadSmoh(channel, "smoh.... (I don't recognize that command.. try !help)");
                    return;
                }
            }

            double xCut = 0;
            double yCut = 0;
            switch (args.length) {
                case 1 -> xCut = 0.25;
                case 2 -> xCut = Double.parseDouble(args[1]);
                case 3 -> {
                    xCut = Double.parseDouble(args[1]);
                    yCut = Double.parseDouble(args[2]);
                }
                default -> {
                    sendSadSmoh(channel, "smoh.. (too many arguments!)");
                    return;
                }
            }

            if (xCut >= attachment.getWidth() || yCut >= attachment.getHeight()) {
                sendSadSmoh(channel, "smoh.. (cut size cannot be larger than the image!)");
                return;
            }

            if (xCut > 1) { // How does this handle cut sizes of 1? Chop off 1 pixel?
                xCut /= attachment.getWidth();
            }
            if (yCut > 1) {
                yCut /= attachment.getHeight();
            }

            if (xCut >= 0 && yCut >= 0) {
                System.out.println("Smoo.. beginning ratio cut!");
                carver.carve(xCut, yCut);
            } else {
                sendSadSmoh(channel, "smoh.... (the cut numbers you gave dont make any sense..)");
                return;
            }
            channel.sendMessage("SMOHOHO (image completed!!!)")
                    .addFile(new File("src/main/resources/images/carved.PNG")).queue();
        } catch (InterruptedException e) {
            sendSadSmoh(channel, "smoh.... (something got interrupted!)");
        } catch (ExecutionException e) {
            sendSadSmoh(channel, "smoh.... (something happened... (Execution Exception!))");
        } catch (NumberFormatException e) {
            sendSadSmoh(channel, "smoh.... (please specify valid numbers (doubles)!!)");
        }
    }

    private boolean isValidCommand(String command) {
        return command.equals("!carve") || command.equals(("!fcarve"));
    }

    private void sendSadSmoh(MessageChannel channel, String msg) {
        channel.sendMessage(msg)
                .addFile(new File("src/main/resources/assets/smoh_apology.jpg")).queue();
    }
}
