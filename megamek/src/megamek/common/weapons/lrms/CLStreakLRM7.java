/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.lrms;

/**
 * @author Sebastian Brocks
 */
public class CLStreakLRM7 extends StreakLRMWeapon {
    private static final long serialVersionUID = 5240577239366457930L;

    public CLStreakLRM7() {
        super();
        name = "Streak LRM 7";
        setInternalName("CLStreakLRM7");
        addLookupName("Clan Streak LRM-7");
        addLookupName("Clan Streak LRM 7");
        heat = 0;
        rackSize = 7;
        shortRange = 7;
        mediumRange = 14;
        longRange = 21;
        extremeRange = 28;
        tonnage = 2.8;
        criticals = 1;
        bv = 120;
        cost = 105000;
        // Per Herb all ProtoMek launcher use the ProtoMek Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        rulesRefs = "327, TO";
        flags = flags.or(F_NO_FIRES).andNot(F_AERO_WEAPON).andNot(F_BA_WEAPON).andNot(F_MEK_WEAPON)
                .andNot(F_TANK_WEAPON).andNot(F_ARTEMIS_COMPATIBLE);
        // Tech Advancement moved to StreakLRMWeapon.java
    }
}
