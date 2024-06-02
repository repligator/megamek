/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ratgenerator;

import megamek.common.*;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * Specific unit variants; analyzes equipment to determine suitability for certain types
 * of missions in addition to what is formally declared in the data files.
 *
 * @author Neoancient
 */
public class ModelRecord extends AbstractUnitRecord {
    public static final int NETWORK_NONE = 0;
    public static final int NETWORK_C3_SLAVE = 1;
    public static final int NETWORK_BA_C3 = 1;
    public static final int NETWORK_C3_MASTER = 1 << 1;
    public static final int NETWORK_C3I = 1 << 2;
    public static final int NETWORK_NAVAL_C3 = 1 << 2;
    public static final int NETWORK_NOVA = 1 << 3;

    public static final int NETWORK_BOOSTED = 1 << 4;
    public static final int NETWORK_COMPANY_COMMAND = 1 << 5;

    public static final int NETWORK_BOOSTED_SLAVE = NETWORK_C3_SLAVE | NETWORK_BOOSTED;
    public static final int NETWORK_BOOSTED_MASTER = NETWORK_C3_MASTER | NETWORK_BOOSTED;

    private MechSummary mechSummary;

    private boolean primitive;
    private boolean retrotech;
    private boolean starLeague;
    private int weightClass;
    private EntityMovementMode movementMode;
    private EnumSet<MissionRole> roles;
    private ArrayList<String> deployedWith;
    private ArrayList<String> requiredUnits;
    private ArrayList<String> excludedFactions;
    private int networkMask;
    private double flak; //proportion of weapon BV that can fire flak ammo

    private double artilleryBVProportion;
    private double longRange; // Proportion of weapons BV with long range and/or indirect fire

    private double srBVProportion; // Proportion of weapons BV with short range
    private int speed;
    private boolean canJump;
    private double ammoRequirement; //used to determine suitability for raider role
    private boolean incendiary; //used to determine suitability for incindiary role
    private boolean apWeapons; //used to determine suitability for anti-infantry role

    private boolean mechanizedBA;
    private boolean magClamp;

    private boolean canAntiMek = false;

    public ModelRecord(String chassis, String model) {
        super(chassis);
        roles = EnumSet.noneOf(MissionRole.class);
        deployedWith = new ArrayList<>();
        requiredUnits = new ArrayList<>();
        excludedFactions = new ArrayList<>();
        networkMask = NETWORK_NONE;
        flak = 0.0;
        longRange = 0.0;
    }

    public ModelRecord(MechSummary ms) {
        this(ms.getFullChassis(), ms.getModel());
        mechSummary = ms;
        unitType = parseUnitType(ms.getUnitType());
        introYear = ms.getYear();

        analyzeModel(ms);

    }

    public String getModel() {
        return mechSummary.getModel();
    }

    public int getWeightClass() {
        return weightClass;
    }

    public EntityMovementMode getMovementMode() {
        return movementMode;
    }

    @Override
    public boolean isClan() {
        return clan;
    }

    /**
     * Unit contains at least some primitive technology, without any advanced tech.
     * Testing is not extensive, there may be units that are not properly flagged.
     * @return   true, if unit contains no advanced tech and at least some primitive tech
     */
    public boolean isPrimitive() {
        return primitive;
    }

    /**
     * Unit consists of at least some primitive technology and some advanced/Star League/Clan
     * technology. Testing is not extensive, there may be units that are not properly flagged.
     * @return   true, if unit contains both primitive and advanced tech
     */
    public boolean isRetrotech () {
        return retrotech;
    }

    public boolean isSL() {
        return starLeague;
    }

    public Set<MissionRole> getRoles() {
        return roles;
    }
    public ArrayList<String> getDeployedWith() {
        return deployedWith;
    }
    public ArrayList<String> getRequiredUnits() {
        return requiredUnits;
    }
    public ArrayList<String> getExcludedFactions() {
        return excludedFactions;
    }
    public int getNetworkMask() {
        return networkMask;
    }
    public void setNetwork(int network) {
        this.networkMask = network;
    }
    public double getFlak() {
        return flak;
    }
    public void setFlak(double flak) {
        this.flak = flak;
    }

    public double getArtilleryProportion () {
        return artilleryBVProportion;
    }

    public double getLongRange() {
        return longRange;
    }

    public double getSRProportion() {
        return srBVProportion;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean getJump() {
        return canJump;
    }

    public double getAmmoRequirement() {
        return ammoRequirement;
    }

    public boolean hasIncendiaryWeapon() {
        return incendiary;
    }

    public boolean hasAPWeapons() {
        return apWeapons;
    }

    public MechSummary getMechSummary() {
        return mechSummary;
    }

    public void addRoles(String str) {
        if (str.isBlank()) {
            roles.clear();
        } else {
            String[] fields = str.split(",");
            for (String role : fields) {
                MissionRole mr = MissionRole.parseRole(role);
                if (mr != null) {
                    roles.add(mr);
                } else {
                    LogManager.getLogger().error("Could not parse mission role for "
                            + getChassis() + " " + getModel() + ": " + role);
                }
            }
        }
    }

    public void setRequiredUnits(String str) {
        String[] subfields = str.split(",");
        for (String unit : subfields) {
            if (unit.startsWith("req:")) {
                requiredUnits.add(unit.replace("req:", ""));
            } else {
                deployedWith.add(unit);
            }
        }
    }

    public void setExcludedFactions(String str) {
        excludedFactions.clear();
        String[] fields = str.split(",");
        for (String faction : fields) {
            excludedFactions.add(faction);
        }
    }

    public boolean factionIsExcluded(FactionRecord fRec) {
        return excludedFactions.contains(fRec.getKey());
    }

    public boolean factionIsExcluded(String faction, String subfaction) {
        if (subfaction == null) {
            return excludedFactions.contains(faction);
        } else {
            return excludedFactions.contains(faction + "." + subfaction);
        }
    }

    @Override
    public String getKey() {
        return mechSummary.getName();
    }

    public boolean canDoMechanizedBA() {
        return mechanizedBA;
    }

    public void setMechanizedBA(boolean mech) {
        mechanizedBA = mech;
    }

    public boolean hasMagClamp() {
        return magClamp;
    }

    public void setMagClamp(boolean magClamp) {
        this.magClamp = magClamp;
    }

    public boolean getAntiMek(){
        return canAntiMek;
    }


    /**
     * Checks the equipment carried by this unit and summarizes it in a variety of easy to access
     * data
     * @param ms   Data for unit
     */
    private void analyzeModel (MechSummary ms) {

        // Basic unit type and movement
        if (unitType == UnitType.MEK) {
            //TODO: id quads and tripods
            movementMode = EntityMovementMode.BIPED;
        } else {
            movementMode = EntityMovementMode.parseFromString(ms.getUnitSubType().toLowerCase());
        }

        if (unitType == UnitType.MEK ||
                unitType == UnitType.TANK ||
                unitType == UnitType.VTOL ||
                unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) {
            omni = ms.getOmni();
        }

        speed = ms.getWalkMp();
        if (ms.getJumpMp() > 0) {
            canJump = true;
            speed++;
        }

        weightClass = ms.getWeightClass();
        // Adjust weight class for support vehicles
        if (weightClass >= EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            if (ms.getTons() <= 39) {
                weightClass = EntityWeightClass.WEIGHT_LIGHT;
            } else if (ms.getTons() <= 59) {
                weightClass = EntityWeightClass.WEIGHT_MEDIUM;
            } else if (ms.getTons() <= 79) {
                weightClass = EntityWeightClass.WEIGHT_HEAVY;
            } else if (ms.getTons() <= 100) {
                weightClass = EntityWeightClass.WEIGHT_ASSAULT;
            } else {
                weightClass = EntityWeightClass.WEIGHT_COLOSSAL;
            }
        }

        double totalBV = 0.0;
        double flakBV = 0.0;
        double artilleryBV = 0.0;
        double lrBV = 0.0;
        double srBV = 0.0;
        int apRating = 0;
        int apThreshold = 6;
        double ammoBV = 0.0;

        // Base technology checks

        boolean losTech = false;
        boolean base_primitive = false;

        clan = ms.isClan();

        // Check if the base unit is primitive
        if (!clan &&
                unitType != UnitType.INFANTRY &&
                unitType != UnitType.BATTLE_ARMOR &&
                unitType != UnitType.PROTOMEK &&
                unitType != UnitType.WARSHIP) {
            base_primitive = isUnitPrimitive(ms);
        }
        // If the unit is not Clan or primitive, then check for if it is lostech (advanced)
        if (!clan &&
                !base_primitive &&
                unitType <= UnitType.AEROSPACEFIGHTER &&
                unitType != UnitType.INFANTRY) {
            losTech = unitHasLostech(ms, false);
        }

        for (int i = 0; i < ms.getEquipmentNames().size(); i++) {

            //EquipmentType.get is throwing an NPE intermittently, and the only possibility I can see
            //is that there is a null equipment name.
            if (null == ms.getEquipmentNames().get(i)) {
                LogManager.getLogger().error(
                        "RATGenerator ModelRecord encountered null equipment name in MechSummary for "
                                + ms.getName() + ", index " + i);
                continue;
            }
            EquipmentType eq = EquipmentType.get(ms.getEquipmentNames().get(i));
            if (eq == null) {
                continue;
            }

            // Only check for lostech equipment if it hasn't already been found
            if (!losTech && !eq.isAvailableIn(3000, false)) {
                losTech = true;
            }

            if (eq instanceof WeaponType) {
                totalBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);

                // Check for C3 master units. These are bit-masked values.
                if (eq.hasFlag(WeaponType.F_C3M)) {
                    networkMask |= NETWORK_C3_MASTER;
                    if (ms.getEquipmentQuantities().get(i) > 1) {
                        networkMask |= NETWORK_COMPANY_COMMAND;
                    }
                    losTech = true;
                    continue;
                } else if (eq.hasFlag(WeaponType.F_C3MBS)) {
                    networkMask |= NETWORK_BOOSTED_MASTER;
                    if (ms.getEquipmentQuantities().get(i) > 1) {
                        networkMask |= NETWORK_COMPANY_COMMAND;
                    }
                    losTech = true;
                    continue;
                }

                // Check for use against airborne targets. Ignore small craft, DropShips, and other
                // large space craft.
                if (unitType < UnitType.SMALL_CRAFT &&
                        !(eq instanceof megamek.common.weapons.SwarmAttack) &&
                        !(eq instanceof megamek.common.weapons.SwarmWeaponAttack) &&
                        !(eq instanceof megamek.common.weapons.LegAttack) &&
                        !(eq instanceof megamek.common.weapons.StopSwarmAttack)) {
                    flakBV += getFlakBVModifier(eq) * eq.getBV(null) * ms.getEquipmentQuantities().get(i);
                }

                // Check for artillery weapons. Ignore aerospace fighters, small craft, and large
                // space craft.
                if (unitType <= UnitType.CONV_FIGHTER &&
                        eq instanceof megamek.common.weapons.artillery.ArtilleryWeapon) {
                    artilleryBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);
                }

                // Don't check incendiary weapons for conventional infantry, fixed wing aircraft,
                // and space-going units
                if (!incendiary &&
                        (unitType < UnitType.CONV_FIGHTER && unitType != UnitType.INFANTRY)) {
                    if (eq instanceof megamek.common.weapons.flamers.FlamerWeapon ||
                            eq instanceof megamek.common.weapons.battlearmor.BAFlamerWeapon ||
                            eq instanceof megamek.common.weapons.ppc.ISPlasmaRifle ||
                            eq instanceof megamek.common.weapons.ppc.CLPlasmaCannon) {
                        incendiary = true;
                    }
                    // Some missile types are capable of being loaded with infernos, which are
                    // highly capable of setting fires
                    if (((WeaponType) eq).getAmmoType() == AmmoType.T_SRM ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_SRM_IMP ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_MML ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_IATM) {
                        incendiary = true;
                    }
                }

                // Don't check anti-personnel weapons for conventional infantry, fixed wing
                // aircraft, and space-going units. Also, don't bother checking if we're
                // already high enough.
                if (apRating < apThreshold &&
                        (unitType < UnitType.CONV_FIGHTER && unitType != UnitType.INFANTRY)) {
                    apRating += getAPRating(eq);
                }

                // Check if a conventional infantry or battle armor unit can perform anti-Mech
                // attacks. This will also pick up battle armor that must first jettison equipment.
                if (!canAntiMek &&
                        (unitType == UnitType.INFANTRY || unitType == UnitType.BATTLE_ARMOR)) {
                    canAntiMek = (eq instanceof megamek.common.weapons.LegAttack ||
                            eq instanceof megamek.common.weapons.SwarmAttack);
                }

                // Total up BV for weapons that require ammo. Streak-type missile systems get a
                // discount. Ignore small craft, DropShips, and large space craft. Ignore infantry
                // weapons except for field guns.
                if (unitType < UnitType.SMALL_CRAFT) {
                    double ammoFactor = 1.0;

                    if (eq instanceof megamek.common.weapons.srms.StreakSRMWeapon ||
                            eq instanceof megamek.common.weapons.lrms.StreakLRMWeapon ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_IATM) {
                        ammoFactor = 0.4;
                    }

                    if (unitType == UnitType.INFANTRY || unitType == UnitType.BATTLE_ARMOR) {
                        if (eq instanceof megamek.common.weapons.infantry.InfantryWeapon) {
                            ammoFactor = 0.0;
                        }
                    }

                    if  (ammoFactor > 0.0 && ((WeaponType) eq).getAmmoType() > megamek.common.AmmoType.T_NA) {
                        ammoBV += eq.getBV(null) * ammoFactor * ms.getEquipmentQuantities().get(i);
                    }

                }

                // Total up BV for weapons capable of attacking at the longest ranges or using
                // indirect fire. Ignore small craft, DropShips, and other space craft.
                if (unitType < UnitType.SMALL_CRAFT) {
                    lrBV += getLongRangeModifier(eq) * eq.getBV(null) * ms.getEquipmentQuantities().get(i);
                }

                // Total up BV of weapons suitable for attacking at close range. Ignore small craft,
                // DropShips, and other space craft. Also skip anti-Mech attacks.
                if (unitType < UnitType.SMALL_CRAFT &&
                        !(eq instanceof megamek.common.weapons.artillery.ArtilleryWeapon) &&
                        !(eq instanceof megamek.common.weapons.SwarmAttack) &&
                        !(eq instanceof megamek.common.weapons.SwarmWeaponAttack) &&
                        !(eq instanceof megamek.common.weapons.LegAttack) &&
                        !(eq instanceof megamek.common.weapons.StopSwarmAttack)) {
                    srBV += getShortRangeModifier(eq) * eq.getBV(null) * ms.getEquipmentQuantities().get(i);
                }

                // Add the spotter role to all units which carry TAG
                if (unitType <= UnitType.AEROSPACEFIGHTER && eq.hasFlag(WeaponType.F_TAG)) {
                    roles.add(MissionRole.SPOTTER);
                    losTech = true;
                }

            // Various non-weapon equipment
            } else if (eq instanceof MiscType) {

                if (eq.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) ||
                        eq.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE) ||
                        eq.hasFlag(MiscType.F_LASER_HEAT_SINK) ||
                        eq.hasFlag(MiscType.F_COMPACT_HEAT_SINK) ||
                        eq.hasFlag(MiscType.F_ECM) ||
                        eq.hasFlag(MiscType.F_ANGEL_ECM) ||
                        eq.hasFlag(MiscType.F_BAP) ||
                        eq.hasFlag(MiscType.F_BLOODHOUND) ||
                        eq.hasFlag(MiscType.F_TARGCOMP) ||
                        eq.hasFlag(MiscType.F_ARTEMIS) ||
                        eq.hasFlag(MiscType.F_ARTEMIS_V) ||
                        eq.hasFlag(MiscType.F_APOLLO)) {
                    losTech = true;
                }
                if (eq.hasFlag(MiscType.F_C3S)) {
                    networkMask |= NETWORK_C3_SLAVE;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_C3I)) {
                    networkMask |= NETWORK_C3I;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_C3SBS)) {
                    networkMask |= NETWORK_BOOSTED_SLAVE;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_NOVA)) {
                    networkMask |= NETWORK_NOVA;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_AP_POD)) {
                    apRating++;
                } else if (eq.hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    magClamp = true;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_UMU)) {
                    movementMode = EntityMovementMode.BIPED_SWIM;
                    losTech = true;
                // Save a bit of time, anything introduced after this date is assumed to be
                // advanced
                } else if (!losTech && (eq.getIntroductionDate() >= 3067)) {
                    losTech = true;
                }

            }
        }

        // Calculate BV proportions for all ground units, VTOL, blue water naval, gun emplacements
        // and fixed wing aircraft. Exclude Small craft, DropShips, and large space craft.
        if (totalBV > 0 && unitType <= UnitType.AEROSPACEFIGHTER) {
            flak = flakBV / totalBV;
            artilleryBVProportion = artilleryBV/totalBV;
            longRange = lrBV / totalBV;
            srBVProportion = srBV / totalBV;
            ammoRequirement = ammoBV / totalBV;

            apWeapons = apRating >= apThreshold;
        }

        // Categorize by technology type
        starLeague = losTech && !clan;
        primitive = base_primitive && !losTech && !clan;
        retrotech = base_primitive && (losTech || clan);


    }

    /**
     * Units are considered primitive if they have no advanced tech and at least some primitive
     * tech. The check is not exhaustive so some niche units may not be flagged properly.
     * @param ms   Unit data
     * @return   true if unit has primitive tech and no advanced tech
     */
    private boolean isUnitPrimitive (MechSummary ms) {

        boolean hasPrimitive = false;

        // Some unit types will not be built with primitive technology
        if (unitType == UnitType.INFANTRY ||
                unitType == UnitType.BATTLE_ARMOR ||
                unitType == UnitType.PROTOMEK ||
                unitType == UnitType.WARSHIP) {
            return false;
        }

        // Primitive engines are not identified, so check for use of advanced types
        int engine_type = ms.getEngineType();
        if (unitType != UnitType.GUN_EMPLACEMENT &&
                unitType != UnitType.SMALL_CRAFT &&
                unitType != UnitType.DROPSHIP &&
                unitType != UnitType.JUMPSHIP &&
                (engine_type == Engine.XL_ENGINE ||
                engine_type == Engine.LIGHT_ENGINE ||
                engine_type == Engine.XXL_ENGINE ||
                engine_type == Engine.COMPACT_ENGINE)) {
            return false;
        }

        // Primitive gyros are not identified, so check for use of advanced types
        if (unitType == UnitType.MEK) {
            if (ms.getGyroType() >= Mech.GYRO_COMPACT) {
                return false;
            }
        }

        // Primitive structure is not identified, so check for use of advanced types
        if (unitType == UnitType.MEK &&
                ms.getInternalsType() >= EquipmentType.T_STRUCTURE_ENDO_STEEL &&
                ms.getInternalsType() <= EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
            return false;
        }
        hasPrimitive = (ms.getInternalsType() == EquipmentType.T_STRUCTURE_INDUSTRIAL);

        // If standard, industrial, or primitive armor is not present, then it must be advanced
        HashSet<Integer> armor_type = ms.getArmorType();
        if (unitType <= UnitType.NAVAL &&
                !armor_type.contains(EquipmentType.T_ARMOR_STANDARD) &&
                !armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE) &&
                !armor_type.contains(EquipmentType.T_ARMOR_INDUSTRIAL) &&
                !armor_type.contains(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL)) {
            return false;
        } else if ((unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER ||
                unitType == UnitType.SMALL_CRAFT ||
                unitType == UnitType.DROPSHIP) &&
                !armor_type.contains(EquipmentType.T_ARMOR_STANDARD) &&
                !armor_type.contains(EquipmentType.T_ARMOR_AEROSPACE) &&
                !armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER) &&
                !armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE_AERO)) {
            return false;
        }
        if (armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE) ||
                armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER) ||
                armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE_AERO)) {
            hasPrimitive = true;
        }

        // Cockpit control systems
        int cockpit_type = ms.getCockpitType();
        if (unitType == UnitType.MEK) {
            if (cockpit_type != Mech.COCKPIT_STANDARD &&
                    cockpit_type != Mech.COCKPIT_INDUSTRIAL &&
                    cockpit_type != Mech.COCKPIT_PRIMITIVE &&
                    cockpit_type != Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                return false;
            } else if (cockpit_type == Mech.COCKPIT_PRIMITIVE ||
                    cockpit_type == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                hasPrimitive = true;
            }
        } else if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (cockpit_type == Aero.COCKPIT_SMALL ||
                    cockpit_type == Aero.COCKPIT_COMMAND_CONSOLE) {
                return false;
            } else if (cockpit_type == Aero.COCKPIT_PRIMITIVE) {
                hasPrimitive = true;
            }
        }

        // If the unit has any primitive tech and nothing but standard tech, it is considered
        // primitive
        return hasPrimitive;
    }

    /**
     * Checks that unit has at least one piece of primitive tech for basic unit components such as
     * engine, frame, and armor. The check is not extensive so some units may include a niche item
     * even though they are not flagged as such.
     * @param ms   Unit data
     * @return     true if unit contains primitive basic equipment
     */
    private boolean unitHasPrimitiveTech(MechSummary ms) {

        // Some unit types will not be built with primitive technology
        if (unitType == UnitType.INFANTRY ||
                unitType == UnitType.BATTLE_ARMOR ||
                unitType == UnitType.PROTOMEK ||
                unitType == UnitType.WARSHIP) {
            return false;
        }

        // Check for non-advanced engines
        int engine_type = ms.getEngineType();
        if (unitType == UnitType.MEK &&
                (engine_type == Engine.COMBUSTION_ENGINE ||
                        engine_type == Engine.FISSION ||
                        engine_type == Engine.BATTERY)) {
            return true;
        } else if ((unitType == UnitType.TANK ||
                unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) &&
                engine_type == Engine.FISSION) {
            return true;
        }

        // Armor
        HashSet<Integer> armor_type = ms.getArmorType();
        if (armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE) ||
                armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER) ||
                armor_type.contains(EquipmentType.T_ARMOR_PRIMITIVE_AERO)) {
            return true;
        }

        // Cockpit/control systems
        if (unitType == UnitType.MEK &&
                (ms.getCockpitType() == Mech.COCKPIT_PRIMITIVE ||
                        ms.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            return true;
        } else if ((unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) && ms.getCockpitType() == Aero.COCKPIT_PRIMITIVE) {
            return true;
        }

        return false;
    }




    /**
     * Check if unit is built with advanced technology. This only checks the basic components,
     * not any mounted equipment such as weapons.
     * @param ms         unit data
     * @param sl_only    true to only check original Star League tech - XL engine, ES internals,
     *                   FF armor
     * @return           true if unit has at least one piece of basic technology
     */
    private boolean unitHasLostech (MechSummary ms, boolean sl_only) {

        // Some units are always considered advanced
        if (unitType == UnitType.BATTLE_ARMOR ||
                unitType == UnitType.PROTOMEK ||
                unitType == UnitType.WARSHIP) {
            return true;
        }

        // Conventional infantry are always basic tech
        if (unitType == UnitType.INFANTRY) {
            return false;
        }

        // Engine check. Star League is limited to XL.
        if (unitType == UnitType.MEK ||
                unitType == UnitType.TANK ||
                unitType == UnitType.VTOL ||
                unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) {
            if (sl_only) {
                if (ms.getEngineType() == Engine.XL_ENGINE) {
                    return true;
                }
            } else if (ms.getEngineType() >= Engine.XL_ENGINE &&
                    ms.getEngineType() <= Engine.COMPACT_ENGINE &&
                    ms.getEngineType() != Engine.FUEL_CELL) {
                return true;
            }
        }

        // Gyro. Star League has no advanced gyro types.
        if (unitType == UnitType.MEK && !sl_only) {
            if (ms.getGyroType() >= Mech.GYRO_COMPACT) {
                return true;
            }
        }

        // Structure. Star League is limited endosteel.
        if (unitType == UnitType.MEK) {
            if (sl_only && ms.getInternalsType() == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
                return true;
            } else if (ms.getInternalsType() >= EquipmentType.T_STRUCTURE_ENDO_STEEL &&
                    ms.getInternalsType() <= EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
                return true;
            }
        }

        // Armor. Star League is limited to simple ferro-fibrous.
        HashSet<Integer> armor_type = ms.getArmorType();
        int base_armor = (int)armor_type.toArray()[0];
        if (unitType <= UnitType.NAVAL) {
            if (sl_only && base_armor == EquipmentType.T_ARMOR_FERRO_FIBROUS) {
                return true;
            } else if ((base_armor >= EquipmentType.T_ARMOR_FERRO_FIBROUS &&
                    base_armor <= EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO) ||
                    base_armor == EquipmentType.T_ARMOR_FERRO_LAMELLOR ||
                    (base_armor >= EquipmentType.T_ARMOR_STEALTH_VEHICLE &&
                            base_armor <= EquipmentType.T_ARMOR_BALLISTIC_REINFORCED)) {
                return true;
            }
        }

        // Cockpit. Star League is limited to command consoles.
        int cockpit_type = ms.getCockpitType();
        if (unitType == UnitType.MEK) {
            if (sl_only && cockpit_type == Mech.COCKPIT_COMMAND_CONSOLE) {
                return true;
            } else if (cockpit_type != Mech.COCKPIT_STANDARD &&
                    cockpit_type != Mech.COCKPIT_PRIMITIVE &&
                    cockpit_type != Mech.COCKPIT_INDUSTRIAL &&
                    cockpit_type != Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                return true;
            }
        } else if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (cockpit_type != Aero.COCKPIT_STANDARD &&
                    cockpit_type != Aero.COCKPIT_PRIMITIVE) {
                return true;
            }
        }

        return false;
    }


    /**
     * Get a BV modifier for a weapon for use against airborne targets. Slightly different checks
     * are provided for fixed wing aircraft for air-to-air combat use
     * @param check_weapon
     * @return   Relative value from zero (not useful) to 1
     */
    private double getFlakBVModifier(EquipmentType check_weapon) {

        double very_effective = 1.0;
        double somewhat_effective = 0.5;
        double not_effective = 0.2;
        double ineffective = 0.0;

        if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (((megamek.common.weapons.Weapon) check_weapon).getAmmoType() == AmmoType.T_AC_LBX ||
                    ((megamek.common.weapons.Weapon) check_weapon).getAmmoType() == AmmoType.T_HAG ||
                    ((megamek.common.weapons.Weapon) check_weapon).getAmmoType() == AmmoType.T_SBGAUSS) {
                return very_effective;
            } else if (((WeaponType) check_weapon).getMedAV() >= 10) {
                return somewhat_effective;
            } else {
                return ineffective;
            }
        }

        if (check_weapon instanceof megamek.common.weapons.artillery.ArtilleryWeapon) {
            return somewhat_effective;
        }
        if (((megamek.common.weapons.Weapon) check_weapon).getAmmoType() == AmmoType.T_AC_LBX ||
                ((megamek.common.weapons.Weapon) check_weapon).getAmmoType() == AmmoType.T_HAG ||
                ((megamek.common.weapons.Weapon) check_weapon).getAmmoType() == AmmoType.T_SBGAUSS ||
                check_weapon instanceof megamek.common.weapons.infantry.InfantrySupportMk2PortableAAWeapon) {
            return very_effective;
        } else if (check_weapon instanceof megamek.common.weapons.infantry.InfantryWeapon ||
                check_weapon instanceof megamek.common.weapons.missiles.RLWeapon) {
            return ineffective;
        } else if (((WeaponType) check_weapon).getLongRange() >= 16) {
            return somewhat_effective;
        } else if (((WeaponType) check_weapon).getMediumRange() >= 8) {
            return not_effective;
        }

        return ineffective;
    }

    /**
     * Evaluates weapons for how effective they are against conventional infantry.
     * Checks are organized to promote early exit for common weapons rather than by value.
     * @param test_weapon  Weapon to check
     * @return             Relative value, 0 is ineffective, higher is more effective
     */
    private int getAPRating(EquipmentType check_weapon) {
        int extremely_effective = 6;
        int very_effective = 4;
        int somewhat_effective = 2;
        int not_effective = 1;
        int ineffective = 0;

        // Common weapons
        if (check_weapon instanceof megamek.common.weapons.mgs.MGWeapon) {
            return very_effective;
        } else if (check_weapon instanceof megamek.common.weapons.srms.SRMWeapon ||
                check_weapon instanceof megamek.common.weapons.missiles.MMLWeapon) {
            return somewhat_effective;
        } else if (check_weapon instanceof megamek.common.weapons.flamers.FlamerWeapon) {
            return extremely_effective;
        }

        // Weapons only found in later eras
        if (check_weapon instanceof megamek.common.weapons.infantry.InfantryWeapon) {
            return not_effective;
        } else if (check_weapon instanceof megamek.common.weapons.battlearmor.BAMGWeapon) {
            return extremely_effective;
        } else if (check_weapon instanceof megamek.common.weapons.battlearmor.BAFlamerWeapon) {
            return extremely_effective;
        } else if (check_weapon instanceof megamek.common.weapons.battlearmor.CLBAMGBearhunterSuperheavy) {
            return extremely_effective;
        } else if (check_weapon instanceof megamek.common.weapons.ppc.ISPlasmaRifle ||
                check_weapon instanceof megamek.common.weapons.ppc.CLPlasmaCannon) {
            return very_effective;
        } else if (check_weapon instanceof megamek.common.weapons.gaussrifles.CLAPGaussRifle) {
            return very_effective;
        } else if (check_weapon instanceof megamek.common.weapons.lasers.ISPulseLaserSmall ||
                    check_weapon instanceof megamek.common.weapons.lasers.CLPulseLaserSmall) {
            return very_effective;
        }

        // Uncommon weapons
        if (check_weapon instanceof megamek.common.weapons.defensivepods.BPodWeapon) {
            return not_effective;
        } else if (check_weapon instanceof megamek.common.weapons.mortars.MekMortarWeapon) {
            return not_effective;
        }

        return ineffective;
    }

    /**
     * Get a BV modifier for a weapon for use at long range
     * @param check_weapon   Weapon to check
     * @return   between zero (not a long ranged weapon) and 1
     */
    private double getLongRangeModifier(EquipmentType check_weapon) {

        double fullRange = 1.0;
        double partialRange = 0.8;
        double minRange = 0.4;
        double shortRange = 0.0;

        if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (((WeaponType) check_weapon).getExtAV() > 0 ||
                    ((WeaponType) check_weapon).getLongAV() > 0 ||
                    check_weapon instanceof megamek.common.weapons.missiles.MMLWeapon ||
                    check_weapon instanceof megamek.common.weapons.missiles.ATMWeapon) {
                return fullRange;
            } else {
                return shortRange;
            }
        }

        // Quick and dirty check for most indirect fire weapons
        boolean isIndirect = ((WeaponType) check_weapon).hasIndirectFire();

        if (((WeaponType) check_weapon).getLongRange() >= 20 ||
                check_weapon instanceof megamek.common.weapons.artillery.ArtilleryWeapon ||
                check_weapon instanceof megamek.common.weapons.missiles.MMLWeapon ||
                check_weapon instanceof megamek.common.weapons.missiles.ATMWeapon) {
            return fullRange;
        } else if (((WeaponType) check_weapon).getMediumRange() >= 14) {
            if (isIndirect) {
                return fullRange;
            } else {
                return partialRange;
            }
        } else if (((WeaponType) check_weapon).getMediumRange() >= 12) {
            if (isIndirect) {
                return partialRange;
            } else {
                return minRange;
            }
        } else if (isIndirect) {
            return minRange;
        }

        return shortRange;
    }

    /**
     * Get a BV modifier for a weapon for use at short range
     * @param check_weapon   Weapon to check
     * @return   between zero (not a short ranged weapon) and 1
     */
    private  double getShortRangeModifier (EquipmentType check_weapon) {

        double shortRange = 1.0;
        double mediumRange = 0.6;
        double longRange = 0.0;

        if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (((WeaponType) check_weapon).getMedAV() == 0 ||
                    check_weapon instanceof megamek.common.weapons.missiles.MMLWeapon ||
                    check_weapon instanceof megamek.common.weapons.missiles.ATMWeapon) {
                return shortRange;
            } else if (((WeaponType) check_weapon).getLongAV() == 0) {
                return mediumRange;
            } else {
                return longRange;
            }
        }

        if (((WeaponType) check_weapon).getMinimumRange() <= 0)
        {
            if (check_weapon instanceof megamek.common.weapons.infantry.InfantryWeapon) {
                if (((WeaponType) check_weapon).getLongRange() <= 6){
                    return shortRange;
                } else if (((WeaponType) check_weapon).getLongRange() <= 12) {
                    return mediumRange;
                }
            }
            if (((WeaponType) check_weapon).getLongRange() <= 15 ||
                    check_weapon instanceof megamek.common.weapons.missiles.MMLWeapon ||
                    check_weapon instanceof megamek.common.weapons.missiles.ATMWeapon) {
                return shortRange;
            }
        } else if (((WeaponType) check_weapon).getMinimumRange() <= 3) {
            if (((WeaponType) check_weapon).getLongRange() <= 15) {
                return mediumRange;
            }
        }

        return longRange;
    }

}

