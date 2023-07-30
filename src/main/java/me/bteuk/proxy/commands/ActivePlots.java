package me.bteuk.proxy.commands;

public class ActivePlots extends PlotListCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     *
     * @param name        Name of the command
     * @param description Description of the command
     */
    public ActivePlots(String name, String description) {
        super(name, description, "Active Plots (Claimed/Submitted)", "SELECT id FROM plot_data WHERE status='claimed' OR status='submitted' OR status='reviewing';", "There are currently no active plots.");
    }
}
