package qowyn.ark.tools;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.stream.JsonGenerator;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.tools.data.AttributeNames;
import qowyn.ark.tools.data.Creature;
import qowyn.ark.tools.data.Item;
import qowyn.ark.types.ArkName;

public class SharedWriters {

  private static void writeFloat(JsonGenerator generator, String name, float value) {
    if (Float.isFinite(value)) {
      generator.write(name, value);
    } else {
      generator.write(name, Float.toString(value));
    }
  }

  public static void writeCreatureInfo(JsonGenerator generator, Creature creature, LatLonCalculator latLongCalculator, GameObjectContainer saveFile, boolean writeAllProperties) {
    writeCreatureInfo(generator, creature, latLongCalculator, saveFile, writeAllProperties, null);
  }

  public static void writeCreatureInfo(JsonGenerator generator, Creature creature, LatLonCalculator latLongCalculator, GameObjectContainer saveFile, boolean writeAllProperties, String fieldName) {
    if (fieldName == null) {
      generator.writeStartObject();
    } else {
      generator.writeStartObject(fieldName);
    }

    if (creature.location != null) {
      writeFloat(generator, "x", creature.location.getX());
      writeFloat(generator, "y", creature.location.getY());
      writeFloat(generator, "z", creature.location.getZ());
      if (latLongCalculator != null) {
        generator.write("lat", Math.round(latLongCalculator.calculateLat(creature.location.getY()) * 10.0) / 10.0);
        generator.write("lon", Math.round(latLongCalculator.calculateLon(creature.location.getX()) * 10.0) / 10.0);
      }
    }

    generator.write("id", creature.dinoId);

    if (writeAllProperties || creature.tamed) {
      generator.write("tamed", creature.tamed);
    }

    if (writeAllProperties || creature.targetingTeam != 0) {
      generator.write("team", creature.targetingTeam);
    }

    if (writeAllProperties || creature.owningPlayerId != 0) {
      generator.write("playerId", creature.owningPlayerId);
    }

    if (writeAllProperties || creature.isFemale) {
      generator.write("female", creature.isFemale);
    }

    for (int i = 0; i < creature.colorSetIndices.length; i++) {
      if (writeAllProperties || creature.colorSetIndices[i] != 0) {
        generator.write("color" + i, Byte.toUnsignedInt(creature.colorSetIndices[i]));
      }
    }

    if (writeAllProperties || creature.tamedAtTime != 0.0) {
      generator.write("tamedAtTime", creature.tamedAtTime);
      if (saveFile instanceof ArkSavegame) {
        generator.write("tamedTime", ((ArkSavegame) saveFile).getGameTime() - creature.tamedAtTime);
      }
    }

    if (writeAllProperties || !creature.tribeName.isEmpty()) {
      generator.write("tribe", creature.tribeName);
    }

    if (writeAllProperties || !creature.tamerString.isEmpty()) {
      generator.write("tamer", creature.tamerString);
    }

    if (writeAllProperties || !creature.owningPlayerName.isEmpty()) {
      generator.write("ownerName", creature.owningPlayerName);
    }

    if (writeAllProperties || !creature.tamedName.isEmpty()) {
      generator.write("name", creature.tamedName);
    }

    if (writeAllProperties || !creature.imprinterName.isEmpty()) {
      generator.write("imprinter", creature.imprinterName);
    }

    generator.write("baseLevel", creature.baseCharacterLevel);

    if (writeAllProperties || creature.baseCharacterLevel > 1) {
      generator.writeStartObject("wildLevels");
      AttributeNames.forEach((index, attrName) -> {
        if (writeAllProperties || creature.numberOfLevelUpPointsApplied[index] != 0) {
          generator.write(attrName, Byte.toUnsignedInt(creature.numberOfLevelUpPointsApplied[index]));
        }
      });
      generator.writeEnd();
    }

    if (writeAllProperties || creature.extraCharacterLevel != 0) {
      generator.write("extraLevel", creature.extraCharacterLevel);
    }

    generator.write("fullLevel", creature.baseCharacterLevel + creature.extraCharacterLevel);

    if (writeAllProperties || creature.extraCharacterLevel != 0) {
      generator.writeStartObject("tamedLevels");
      AttributeNames.forEach((index, attrName) -> {
        if (writeAllProperties || creature.numberOfLevelUpPointsAppliedTamed[index] != 0) {
          generator.write(attrName, Byte.toUnsignedInt(creature.numberOfLevelUpPointsAppliedTamed[index]));
        }
      });
      generator.writeEnd();
    }

    if (writeAllProperties || creature.experiencePoints != 0.0f) {
      generator.write("experience", creature.experiencePoints);
    }

    if (writeAllProperties || creature.dinoImprintingQuality != 0.0f) {
      generator.write("imprintingQuality", creature.dinoImprintingQuality);
    }

    generator.writeEnd();
  }

  public static void writeInventorySummary(JsonGenerator generator, List<Item> items, String objName) {
    Map<ArkName, Integer> itemMap = new HashMap<>();

    for (Item item : items) {
      itemMap.merge(item.className, item.quantity, Integer::sum);
    }

    generator.writeStartArray(objName);

    itemMap.entrySet().stream().sorted(comparing(Map.Entry::getValue, reverseOrder())).forEach(e -> {
      generator.writeStartObject();

      String name = e.getKey().toString();
      if (DataManager.hasItem(name)) {
        name = DataManager.getItem(name).getName();
      }

      generator.write("name", name);
      generator.write("count", e.getValue());

      generator.writeEnd();
    });

    generator.writeEnd();
  }

  public static void writeInventoryLong(JsonGenerator generator, List<Item> items, String objName) {
    writeInventoryLong(generator, items, objName, false);
  }

  public static void writeInventoryLong(JsonGenerator generator, List<Item> items, String objName, boolean blueprintStatus) {
    generator.writeStartArray(objName);

    items.sort(Comparator.comparing(item -> DataManager.hasItem(item.className.toString()) ? DataManager.getItem(item.className.toString()).getName() : item.className.toString()));
    for (Item item : items) {
      generator.writeStartObject();

      String name = item.className.toString();
      if (DataManager.hasItem(name)) {
        name = DataManager.getItem(name).getName();
      }

      generator.write("name", name);

      if (blueprintStatus) {
        generator.write("isBlueprint", item.isBlueprint);
      }

      if (item.quantity > 1) {
        generator.write("quantity", item.quantity);
      }

      if (!item.customName.isEmpty()) {
        generator.write("customName", item.customName);
      }

      if (!item.customDescription.isEmpty()) {
        generator.write("customDescription", item.customDescription);
      }

      if (!item.isBlueprint && item.durability > 0.0f) {
        generator.write("durability", item.durability);
      }

      if (item.rating > 0.0f) {
        generator.write("rating", item.rating);
      }

      if (item.quality > 0) {
        generator.write("quality", item.quality);
      }

      if (item.itemStatValues[1] != 0) {
        generator.write("armorMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[1])) * 0.2f * 0.001f);
      }

      if (item.itemStatValues[2] != 0) {
        generator.write("durabilityMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[2])) * 0.25f * 0.001f);
      }

      if (item.itemStatValues[3] != 0) {
        generator.write("damageMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[3])) * 0.1f * 0.001f);
      }

      if (item.itemStatValues[5] != 0) {
        generator.write("hypoMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[5])) * 0.2f * 0.001f);
      }

      if (item.itemStatValues[7] != 0) {
        generator.write("hyperMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[7])) * 0.2f * 0.001f);
      }

      if (item.className.toString().contains("_Fertilized_")) {
        generator.writeStartObject("eggAttributes");

        for (int i = 0; i < item.eggLevelups.length; i++) {
          byte value = item.eggLevelups[i];
          if (value != 0) {
            generator.write(AttributeNames.get(i), Byte.toUnsignedInt(value));
          }
        }

        generator.writeEnd();

        generator.writeStartObject("eggColors");

        for (int i = 0; i < item.eggColors.length; i++) {
          byte value = item.eggColors[i];
          if (value != 0) {
            generator.write(Integer.toString(i), Byte.toUnsignedInt(value));
          }
        }

        generator.writeEnd();
      }

      generator.writeEnd();
    }
    generator.writeEnd();
  }

}
