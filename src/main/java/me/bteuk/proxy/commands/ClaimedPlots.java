package me.bteuk.proxy.commands;

public class ClaimedPlots extends PlotListCommand {

    /**
     * Constructor, saved the name and description of the command.
     * Also registers the command in Discord.
     *
     * @param name        Name of the command
     * @param description Description of the command
     */
    public ClaimedPlots(String name, String description) {
        super(name, description, "Claimed Plots", "", "There are currently no claimed plots.");
    }

}
