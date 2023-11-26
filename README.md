<div style="text-align: center;"><h1 >Farm All Night</h1>
<br />

![Minecraft Versions](https://img.shields.io/badge/Minecraft-1.20.2-9450cc)
![Minecraft Loader](https://img.shields.io/badge/Loader-Fabric-547280)
<br/>

</div>

<blockquote style="text-align: center;">Farm All Night is a utility mod to enhance the experience of farming crops.
</blockquote>

<h3 align="center">
<a href="https://modrinth.com/mod/farm-all-night" target="_blank">Modrinth Homepage</a>
</h3>

## Usage
The simple chat commands can be used to control and fine tune the settings.
Each setting can be controlled with the corresponding keyword:
| **Setting**                                             | **Keyword**        |
|---------------------------------------------------------|--------------------|
| How many crops can be harvested per tick.               | maxHarvestsPerTick |
| Whether or not to automatically pick up dropped items.  | autoPickupItems    |
| How many blocks away to detect crops from.              | harvestRange       |
| How many crops to wait for before harvest.              | batchSize          |
| Whether or not to play a ding when a crop is harvested. | harvestSound       |
| Whether or not to log out when the inventory is full.   | logOnInvFull       |
| Whether or not to auto-plant crops on empty farmland.   | autoPlant          |

For example, to disable the harvest sound, the command would be `/farmutils harvestSound false`.
Once you have finished customising the options, run `/farmutils start` to start the harvest.
Similarly, `/farmutils stop` will stop it.
