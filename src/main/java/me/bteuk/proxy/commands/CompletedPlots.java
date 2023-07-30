package me.bteuk.proxy.commands;

public class CompletedPlots extends PlotListCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     *
     * @param name        Name of the command
     * @param description Description of the command
     */
    public CompletedPlots(String name, String description) {
        super(name, description, "Completed Plots", "SELECT id FROM plot_data WHERE status='completed';", "There are currently no completed plots.");
    }
}
