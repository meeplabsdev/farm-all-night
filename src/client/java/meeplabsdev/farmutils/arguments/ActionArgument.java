package meeplabsdev.farmutils.arguments;

public enum ActionArgument {
    help("help"),
    _perTick("maxHarvestsPerTick"),
    _shouldPickupItems("autoPickupItems"),
    _harvestRange("harvestRange"),
    _batchSize("batchSize"),
    _playSound("harvestSound");

    public final String action;

    ActionArgument(String action) {
        this.action = action;
    }
}
