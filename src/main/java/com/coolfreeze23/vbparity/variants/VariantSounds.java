package com.coolfreeze23.vbparity.variants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events that native 26.2 has and VanillaBackport 1.2.2 does not register: the baby sound sets of wolves,
 * pigs, chickens and cats, and the moody cow, big/mini pig, picky chicken and royal cat sound variants.
 * They use the {@code minecraft} namespace so their ids match native and {@code assets/minecraft/sounds.json}.
 */
public final class VariantSounds {
    private static final List<SoundEvent> ALL = new ArrayList<>();

    public static final SoundEvent BABY_WOLF_AMBIENT = create("entity.baby_wolf.ambient");
    public static final SoundEvent BABY_WOLF_DEATH = create("entity.baby_wolf.death");
    public static final SoundEvent BABY_WOLF_GROWL = create("entity.baby_wolf.growl");
    public static final SoundEvent BABY_WOLF_HURT = create("entity.baby_wolf.hurt");
    public static final SoundEvent BABY_WOLF_PANT = create("entity.baby_wolf.pant");
    public static final SoundEvent BABY_WOLF_STEP = create("entity.baby_wolf.step");
    public static final SoundEvent BABY_WOLF_WHINE = create("entity.baby_wolf.whine");

    public static final SoundEvent COW_MOODY_AMBIENT = create("entity.cow_moody.ambient");
    public static final SoundEvent COW_MOODY_DEATH = create("entity.cow_moody.death");
    public static final SoundEvent COW_MOODY_HURT = create("entity.cow_moody.hurt");
    public static final SoundEvent COW_MOODY_STEP = create("entity.cow_moody.step");

    public static final SoundEvent PIG_EAT = create("entity.pig.eat");
    public static final SoundEvent PIG_BIG_AMBIENT = create("entity.pig_big.ambient");
    public static final SoundEvent PIG_BIG_DEATH = create("entity.pig_big.death");
    public static final SoundEvent PIG_BIG_EAT = create("entity.pig_big.eat");
    public static final SoundEvent PIG_BIG_HURT = create("entity.pig_big.hurt");
    public static final SoundEvent PIG_MINI_AMBIENT = create("entity.pig_mini.ambient");
    public static final SoundEvent PIG_MINI_DEATH = create("entity.pig_mini.death");
    public static final SoundEvent PIG_MINI_EAT = create("entity.pig_mini.eat");
    public static final SoundEvent PIG_MINI_HURT = create("entity.pig_mini.hurt");
    public static final SoundEvent BABY_PIG_AMBIENT = create("entity.baby_pig.ambient");
    public static final SoundEvent BABY_PIG_DEATH = create("entity.baby_pig.death");
    public static final SoundEvent BABY_PIG_EAT = create("entity.baby_pig.eat");
    public static final SoundEvent BABY_PIG_HURT = create("entity.baby_pig.hurt");
    public static final SoundEvent BABY_PIG_STEP = create("entity.baby_pig.step");

    public static final SoundEvent CHICKEN_PICKY_AMBIENT = create("entity.chicken_picky.ambient");
    public static final SoundEvent CHICKEN_PICKY_DEATH = create("entity.chicken_picky.death");
    public static final SoundEvent CHICKEN_PICKY_HURT = create("entity.chicken_picky.hurt");
    public static final SoundEvent CHICKEN_PICKY_STEP = create("entity.chicken_picky.step");
    public static final SoundEvent BABY_CHICKEN_AMBIENT = create("entity.baby_chicken.ambient");
    public static final SoundEvent BABY_CHICKEN_DEATH = create("entity.baby_chicken.death");
    public static final SoundEvent BABY_CHICKEN_HURT = create("entity.baby_chicken.hurt");
    public static final SoundEvent BABY_CHICKEN_STEP = create("entity.baby_chicken.step");

    public static final SoundEvent CAT_ROYAL_AMBIENT = create("entity.cat_royal.ambient");
    public static final SoundEvent CAT_ROYAL_BEG_FOR_FOOD = create("entity.cat_royal.beg_for_food");
    public static final SoundEvent CAT_ROYAL_DEATH = create("entity.cat_royal.death");
    public static final SoundEvent CAT_ROYAL_EAT = create("entity.cat_royal.eat");
    public static final SoundEvent CAT_ROYAL_HISS = create("entity.cat_royal.hiss");
    public static final SoundEvent CAT_ROYAL_HURT = create("entity.cat_royal.hurt");
    public static final SoundEvent CAT_ROYAL_PURR = create("entity.cat_royal.purr");
    public static final SoundEvent CAT_ROYAL_PURREOW = create("entity.cat_royal.purreow");
    public static final SoundEvent CAT_ROYAL_STRAY_AMBIENT = create("entity.cat_royal.stray_ambient");
    public static final SoundEvent BABY_CAT_AMBIENT = create("entity.baby_cat.ambient");
    public static final SoundEvent BABY_CAT_BEG_FOR_FOOD = create("entity.baby_cat.beg_for_food");
    public static final SoundEvent BABY_CAT_DEATH = create("entity.baby_cat.death");
    public static final SoundEvent BABY_CAT_EAT = create("entity.baby_cat.eat");
    public static final SoundEvent BABY_CAT_HISS = create("entity.baby_cat.hiss");
    public static final SoundEvent BABY_CAT_HURT = create("entity.baby_cat.hurt");
    public static final SoundEvent BABY_CAT_PURR = create("entity.baby_cat.purr");
    public static final SoundEvent BABY_CAT_PURREOW = create("entity.baby_cat.purreow");
    public static final SoundEvent BABY_CAT_STRAY_AMBIENT = create("entity.baby_cat.stray_ambient");

    private VariantSounds() {
    }

    private static SoundEvent create(String path) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(path));
        ALL.add(event);
        return event;
    }

    public static List<SoundEvent> all() {
        return Collections.unmodifiableList(ALL);
    }
}
