package com.matteopaciolla.portroyal.core.cards.expeditions;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.employees.*;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Expedition extends Card {
    private final int points;
    private final int money;
    private final Map<ExpeditionEmployee, Integer> employeesTypeCount;

    public Expedition(int id, int money, int points, int captainsNumber, int priestsNumber, int settlersNumber) {
        super(id);
        this.points = points;
        this.money = money;
        this.employeesTypeCount = Map.of(
                ExpeditionEmployee.CAPTAIN, captainsNumber,
                ExpeditionEmployee.PRIEST, priestsNumber,
                ExpeditionEmployee.SETTLER, settlersNumber
        );
    }

    public int getCaptainsNumber() {
        return employeesTypeCount.get(ExpeditionEmployee.CAPTAIN);
    }

    public int getPriestsNumber() {
        return employeesTypeCount.get(ExpeditionEmployee.PRIEST);
    }

    public int getSettlersNumber() {
        return employeesTypeCount.get(ExpeditionEmployee.SETTLER);
    }

    public List<ExpeditionEmployee> getNeededEmployeesTypesList() {
        List<ExpeditionEmployee> employeesTypes = new LinkedList<>();
        for (Map.Entry<ExpeditionEmployee, Integer> entry : employeesTypeCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                employeesTypes.add(entry.getKey());
            }
        }
        return employeesTypes;
    }

    public List<ExpeditionEmployee> getPossibleEmployeesTypesList(Player player) {
        List<ExpeditionEmployee> employeesTypes = new LinkedList<>();

        int[] employeesAvailable = new int[]{
                player.getEmployeeClassNumber(Captain.class),
                player.getEmployeeClassNumber(Priest.class),
                player.getEmployeeClassNumber(Settler.class),
                player.getEmployeeClassNumber(Handyman.class)
        };

        int substituteCount = employeesAvailable[3];

        int[] neededEmployees = new int[]{
                employeesTypeCount.get(ExpeditionEmployee.CAPTAIN),
                employeesTypeCount.get(ExpeditionEmployee.PRIEST),
                employeesTypeCount.get(ExpeditionEmployee.SETTLER)
        };

        ExpeditionEmployee[] employeeTypes = new ExpeditionEmployee[]{
                ExpeditionEmployee.CAPTAIN,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.SETTLER
        };

        for (int i = 0; i < neededEmployees.length; i++) {
            for (int j = 0; j < neededEmployees[i]; j++) {
                if (employeesAvailable[i] > 0) {
                    employeesTypes.add(employeeTypes[i]);
                    employeesAvailable[i]--;
                } else {
                    if (substituteCount > 0) {
                        employeesTypes.add(ExpeditionEmployee.HANDYMAN);
                        substituteCount--;
                    } else {
                        throw new RuntimeException("Player does not have enough employees to commit the expedition");
                    }
                }
            }
        }
        return employeesTypes;
    }

    @Override
    public String toString() {
        String empStringRep = Emojis.CAPTAIN.repeat(Math.max(0, getCaptainsNumber())) +
                Emojis.PRIEST.repeat(Math.max(0, getPriestsNumber())) +
                Emojis.SETTLER.repeat(Math.max(0, getSettlersNumber()));
        String template = "%s %d" + Emojis.POINTS + "%d" + Emojis.MONEY + " %s";
        return String.format(template, super.toString(), points, money, empStringRep);
    }

    @Override
    public String getIcon() {
        return Emojis.EXPEDITION;
    }

    public boolean isPlayerAbleToCommitExpedition(Player player, boolean considerHandymen) {
        Map<ExpeditionEmployee, Integer> availableEmployeesTypeCount = Map.of(
                ExpeditionEmployee.CAPTAIN, player.getEmployeeClassNumber(Captain.class),
                ExpeditionEmployee.PRIEST, player.getEmployeeClassNumber(Priest.class),
                ExpeditionEmployee.SETTLER, player.getEmployeeClassNumber(Settler.class),
                ExpeditionEmployee.HANDYMAN, considerHandymen ? player.getEmployeeClassNumber(Handyman.class) : 0
        );
        return isCommittable(availableEmployeesTypeCount);
    }

    public boolean isCommittable(Map<ExpeditionEmployee, Integer> availableEmployeesTypeCount){
        int[] employeesAvailable = new int[]{
                availableEmployeesTypeCount.getOrDefault(ExpeditionEmployee.CAPTAIN, 0),
                availableEmployeesTypeCount.getOrDefault(ExpeditionEmployee.PRIEST, 0),
                availableEmployeesTypeCount.getOrDefault(ExpeditionEmployee.SETTLER, 0),
                availableEmployeesTypeCount.getOrDefault(ExpeditionEmployee.HANDYMAN, 0)
        };

        int[] neededEmployees = new int[]{
                employeesTypeCount.get(ExpeditionEmployee.CAPTAIN),
                employeesTypeCount.get(ExpeditionEmployee.PRIEST),
                employeesTypeCount.get(ExpeditionEmployee.SETTLER)
        };
        // The last element in the array is the count of the handymen
        int substituteCount = employeesAvailable[3];
        for (int i = 0; i < neededEmployees.length; i++) {
            // Check if we have enough of the specific type
            if (employeesAvailable[i] >= neededEmployees[i]) {
                employeesAvailable[i] -= neededEmployees[i];
            } else {
                // Use the 4th type to fill in the shortage
                int shortage = neededEmployees[i] - employeesAvailable[i];
                if (substituteCount >= shortage) {
                    substituteCount -= shortage;
                    employeesAvailable[i] = 0;
                } else {
                    return false; // Not enough employees to commit the expedition
                }
            }
        }
        return true;
    }

    public static Map<ExpeditionEmployee, Integer> getEmpTypesCountFromList(List<EmployeeCard> employeeCards) {
        return Map.of(
                ExpeditionEmployee.CAPTAIN, (int) employeeCards.stream().filter(card -> card instanceof Captain).count(),
                ExpeditionEmployee.PRIEST, (int) employeeCards.stream().filter(card -> card instanceof Priest).count(),
                ExpeditionEmployee.SETTLER, (int) employeeCards.stream().filter(card -> card instanceof Settler).count(),
                ExpeditionEmployee.HANDYMAN, (int) employeeCards.stream().filter(card -> card instanceof Handyman).count()
        );
    }

    public static Map<ExpeditionEmployee, Integer> getEmpTypesCountFromTypesList(List<ExpeditionEmployee> employeeTypes) {
        return employeeTypes.stream()
                .collect(Collectors.toMap(employee -> employee, employee -> 1, Integer::sum));
    }
}
