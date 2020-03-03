package Client;

import Client.Model.*;

import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.util.*;
import java.util.Map;

/**
 * You must put your code in this class {@link AI}.
 * This class has {@link #pick}, to choose units before the start of the game;
 * {@link #turn}, to do orders while game is running;
 * {@link #end}, to process after the end of the game;
 */

public class AI {
    private Random random = new Random();
    private World world, lastWorld;
    private int selectedPath;

    public void pick(World world) {
        System.out.println("pick started");

        this.world = world;

        List<BaseUnit> myHand = new ArrayList<>();
        myHand.add(world.getBaseUnitById(0));
        myHand.add(world.getBaseUnitById(1));
        myHand.add(world.getBaseUnitById(2));
        myHand.add(world.getBaseUnitById(6));
        myHand.add(world.getBaseUnitById(7));

        // picking the chosen hand - rest of the hand will automatically be filled with random baseUnits
        world.chooseHand(myHand);

        preProcess();
        System.out.println("----------------------");
    }

    private void preProcess(){
        selectedPath = findShortestPath();
        for(BaseUnit unit1 : world.getAllBaseUnits()){
            for(BaseUnit unit2 : world.getAllBaseUnits()){
                if(unit1.getTargetType() == UnitTarget.BOTH){
                    unitsTargetedBy[unit1.getTypeId()].add(unit2.getTypeId());
                    unitsTargeting[unit2.getTypeId()].add(unit1.getTypeId());
                }
                if(unit1.getTargetType() == UnitTarget.GROUND && !unit1.isFlying()){
                    unitsTargetedBy[unit1.getTypeId()].add(unit2.getTypeId());
                    unitsTargeting[unit2.getTypeId()].add(unit1.getTypeId());
                }
                if(unit1.getTargetType() == UnitTarget.AIR && unit1.isFlying()){
                    unitsTargetedBy[unit1.getTypeId()].add(unit2.getTypeId());
                    unitsTargeting[unit2.getTypeId()].add(unit1.getTypeId());
                }
            }
        }
    }

    private Player me, friend, en1, en2;
    private int AP;
    private List<BaseUnit> Hand;
    private Client.Model.Map map;

    private List<Integer>[] unitsTargetedBy = new List[]{new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};
    private List<Integer>[] unitsTargeting = new List[]{new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList()};

    private List<Integer> myPaths = new ArrayList<>(), friendPaths = new ArrayList<>();
    private Map<Integer, List<Integer>> enemyUnitsPaths = new HashMap<>();
    private Map<Integer, Set<Integer>> enemyUnitsTargetKing = new HashMap<>();
    private List<Unit> enemyAliveUnits = new ArrayList<>();

    private List<Integer> weightedUnits = new ArrayList<>();

    private void findEnemyUnitsPaths(Player first, Player second){
        for(Unit unit : first.getUnits()){
            List<Integer> PathIds = new ArrayList<>();
            Set<Integer> TargetKingIds = new HashSet<>();
            if(enemyUnitsPaths.containsKey(unit.getUnitId())){
                for(Path path : world.getPathsCrossingCell(unit.getCell())){
                    if(enemyUnitsPaths.get(unit.getUnitId()).contains(path.getId())){
                        PathIds.add(path.getId());
                        if(myPaths.contains(path.getId())){
                            TargetKingIds.add(me.getPlayerId());
                        }
                        if(friendPaths.contains(path.getId())){
                            TargetKingIds.add(friend.getPlayerId());
                        }
                    }
                }
                if(PathIds.size() > 0){
                    enemyUnitsPaths.replace(unit.getUnitId(), PathIds);
                    enemyUnitsTargetKing.replace(unit.getUnitId(), TargetKingIds);
                }
            }
            else{
                for(Path path : world.getPathsCrossingCell(unit.getCell())){
                    if(path.getId() != first.getPathToFriend().getId()) {
                        PathIds.add(path.getId());
                        if(myPaths.contains(path.getId())){
                            TargetKingIds.add(me.getPlayerId());
                        }
                        if(friendPaths.contains(path.getId())){
                            TargetKingIds.add(friend.getPlayerId());
                        }
                    }
                }
                if(PathIds.size() == 0){
                    for(Path path : second.getPathsFromPlayer()){
                        PathIds.add(path.getId());
                        if(myPaths.contains(path.getId())){
                            TargetKingIds.add(me.getPlayerId());
                        }
                        if(friendPaths.contains(path.getId())){
                            TargetKingIds.add(friend.getPlayerId());
                        }
                    }
                }
                enemyUnitsPaths.put(unit.getUnitId(), PathIds);
                enemyUnitsTargetKing.put(unit.getUnitId(), TargetKingIds);
            }
        }
    }

    private Path getEnemyPath(int id){
        for(Path path : en1.getPathsFromPlayer()){
            if(path.getId() == id){
                return path;
            }
        }
        for(Path path : en2.getPathsFromPlayer()){
            if(path.getId() == id){
                return path;
            }
        }
        return null;
    }

    private double unitTargetingMeProbability(Unit unit){
        double n = enemyUnitsPaths.get(unit.getUnitId()).size(), i = 0;
        for(int pathId : enemyUnitsPaths.get(unit.getUnitId())){
            if(myPaths.contains(pathId)){
                i++;
            }
        }
        return i / n;
    }

    public void turn(World world) {
        System.out.println("turn: " + world.getCurrentTurn());

        this.world = world;
        me = world.getMe();
        friend = world.getFriend();
        en1 = world.getFirstEnemy();
        en2 = world.getSecondEnemy();
        map = world.getMap();

        if(!me.isAlive()) {
            System.out.println("==== I'm dead ====");
        }

        parse();

        if(weightedUnits.size() > 0){
            int n = weightedUnits.get(random.nextInt(weightedUnits.size()));
            System.out.println("put: " + (n == 9 ? "nothing" : n));
            world.putUnit(n, selectedPath);
        }

        doSpell();
        upgrade();

        lastWorld = world;
        System.out.println("----------------------");
    }

    public void parse(){
        AP = me.getAp();
        Hand = me.getHand();
        myPaths.clear();
        friendPaths.clear();
        for(Path path : me.getPathsFromPlayer()){
            myPaths.add(path.getId());
        }
        for(Path path : friend.getPathsFromPlayer()){
            if(friend.isAlive()) {
                friendPaths.add(path.getId());
            }
            else {
                myPaths.add(path.getId());
            }
        }
        System.out.println("myPaths: " + Arrays.toString(myPaths.toArray()));
        System.out.println("friendPaths: " + Arrays.toString(friendPaths.toArray()));
        findEnemyUnitsPaths(en1, en2);
        findEnemyUnitsPaths(en2, en1);
        for(Unit unit : enemyAliveUnits){
            System.out.println("unit " + unit.getUnitId() + " paths: " + Arrays.toString(enemyUnitsPaths.get(unit.getUnitId()).toArray()));
        }

        enemyAliveUnits.clear();
        enemyAliveUnits.addAll(en1.getUnits());
        enemyAliveUnits.addAll(en2.getUnits());
        calcWeights();

        if(isCrisis()){
            selectedPath = findDefencePath();
        }
        else{
            selectedPath = findShortestPath();
        }
        System.out.println("*** selectedPath: " + selectedPath);
    }

    public int findShortestPath(){
        Path shortestPath = world.getMe().getPathsFromPlayer().get(0);
        for(Path path : world.getMe().getPathsFromPlayer()){
            if(path.getCells().size() < shortestPath.getCells().size()){
                shortestPath = path;
            }
        }
        return shortestPath.getId();
    }

    public int findDefencePath(){
        Map<Integer, Integer> pathWeight = new HashMap<>();
        Map<Integer, Cell> furthestEnemy = new HashMap<>();
        for(Path path : me.getPathsFromPlayer()){
            pathWeight.put(path.getId(), 0);
            furthestEnemy.put(path.getId(), path.getCells().get(0));
        }
        for(Path path : friend.getPathsFromPlayer()){
            pathWeight.put(path.getId(), 0);
            furthestEnemy.put(path.getId(), me.getPathToFriend().getCells().get(0));
        }
        for(Unit enemyUnit : enemyAliveUnits){
            if(isCrisisUnit(enemyUnit)){
                for(int pathId : enemyUnitsPaths.get(enemyUnit.getUnitId())){
                    pathWeight.replace(pathId, pathWeight.get(pathId) + 3 * enemyUnit.getAttack() + enemyUnit.getHp());
                    if(me.getKing().getDistance(enemyUnit) > me.getKing().getDistance(furthestEnemy.get(pathId))){
                        furthestEnemy.replace(pathId, enemyUnit.getCell());
                    }
                }
            }
        }
        for(Path path : me.getPathsFromPlayer()){
            int pathId = path.getId();
            for (Cell cell : path.getCells()){
                for(Unit unit : cell.getUnits()){
                    if(unit.getPlayerId() == me.getPlayerId() || unit.getPlayerId() == friend.getPlayerId()){
                        pathWeight.replace(pathId, pathWeight.get(pathId) - 3 * unit.getAttack() - unit.getHp());
                    }
                }
                if(cell.equals(furthestEnemy.get(path.getId()))){
                    break;
                }
            }
        }
        for(Path path : friend.getPathsFromPlayer()){
            int pathId = path.getId();
            int weight = pathWeight.get(pathId);
            boolean found = false;
            for (Cell cell : me.getPathToFriend().getCells()){
                for(Unit unit : cell.getUnits()){
                    if(unit.getPlayerId() == me.getPlayerId() || unit.getPlayerId() == friend.getPlayerId()){
                        weight -= 3 * unit.getAttack() + unit.getHp();
                    }
                }
                if(cell.equals(furthestEnemy.get(path.getId()))){
                    found = true;
                    break;
                }
            }
            if(found) {
                pathWeight.replace(pathId, weight);
            }
            else {
                weight = pathWeight.get(pathId);
                for (Cell cell : path.getCells()){
                    for(Unit unit : cell.getUnits()){
                        if(unit.getPlayerId() == me.getPlayerId() || unit.getPlayerId() == friend.getPlayerId()){
                            weight -= 3 * unit.getAttack() + unit.getHp();
                        }
                    }
                    if(cell.equals(furthestEnemy.get(path.getId()))){
                        found = true;
                        break;
                    }
                }
                if(found) {
                    pathWeight.replace(pathId, weight);
                }
            }
        }
        return Collections.max(pathWeight.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
    }

    public void calcWeights(){
        if(isCrisis()){
            System.out.println("### crisis mode!");
            calcDefenceWeights();
        }
        else{
            System.out.println("### normal mode!");
            calcAttackWeights();
        }
        for(int i = 0; i < 9; i++) {
            System.out.println("weight " + i + ":\t" + Collections.frequency(weightedUnits, i));
        }
    }

    public double[] balanceWeights(double[] weight){
        Map<Double, Integer> units = new TreeMap<>(Collections.reverseOrder());
        for(int i = 0; i < 9; i++){
            units.put(weight[i], i);
        }
        int zarib = 8;
        for(Map.Entry<Double, Integer> unit : units.entrySet()){
            if(zarib > 0){
                weight[unit.getValue()] *= zarib;
                zarib /= 2;
            }
        }
        return weight;
    }

    public void calcAttackWeights(){
        weightedUnits.clear();
        double[] weight = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        double weightSum = 0;
        for(BaseUnit unit : Hand){
            weight[unit.getTypeId()] = unit.getBaseRange() * 100 + unit.getBaseAttack() * 50 + (unit.isMultiple() ? 200 : 100) + (unit.getTargetType() == UnitTarget.BOTH ? 100 : 0) + (double)unit.getMaxHp() / 2.5 * 40 - unit.getAp() * 100;
            for(Unit enemyUnit : enemyAliveUnits){
                if(unitsTargetedBy[unit.getTypeId()].contains(enemyUnit.getBaseUnit().getTypeId())){
                    weight[unit.getTypeId()] *= 1 + unitTargetingMeProbability(enemyUnit) * enemyUnit.getAttack() / 50 * unit.getBaseAttack() / enemyUnit.getHp();
                }
                if(unitsTargeting[unit.getTypeId()].contains(enemyUnit.getBaseUnit().getTypeId())){
                    weight[unit.getTypeId()] *= 1 - unitTargetingMeProbability(enemyUnit) * enemyUnit.getAttack() / unit.getMaxHp() / 2;
                }
            }
            if(unit.getAp() > AP){
                weight[unit.getTypeId()] = 0;
            }
            weightSum += weight[unit.getTypeId()];
        }
        weight = balanceWeights(weight);
        if(weightSum > 0){
            weight[9] = ((double) (world.getGameConstants().getMaxAP() - AP) / AP) * weightSum;
        }
        for(int i = 0; i < 10; i++){
            for(int c = 0; c < weight[i]; c++){
                weightedUnits.add(i);
            }
        }
    }

    public void calcDefenceWeights() {
        weightedUnits.clear();
        double[] weight = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        boolean isKingUnderAttack = false, isAllZero = true;

        for (Unit enemyUnit : enemyAliveUnits) {
            if(me.getKing().getDistance(enemyUnit) <= enemyUnit.getRange()) {
                isKingUnderAttack = true;
            }
        }

        if(!isKingUnderAttack){
            for (BaseUnit unit : Hand) {
                if(AP >= unit.getAp()) {
                    weight[unit.getTypeId()] = 1;
                }
            }
            for (Unit enemyUnit : enemyAliveUnits) {
                if (!isCrisisUnit(enemyUnit)) {
                    continue;
                }
                int dis = me.getKing().getCenter().getDistance(enemyUnit.getCell());
                int time = me.getKing().getDistance(enemyUnit) - enemyUnit.getRange();
                for (BaseUnit unit : Hand) {
                    if (dis - 2 * time > unit.getBaseRange()) {
                        weight[unit.getTypeId()] = 0;
                    }
                }
            }
            for(int i = 0; i < 10; i++) {
                if (weight[i] != 0) {
                    isAllZero = false;
                }
            }
        }
        if(isKingUnderAttack || isAllZero) {
            for (BaseUnit unit : Hand) {
                if(AP >= unit.getAp()){
                    weight[unit.getTypeId()] = 2 * unit.getBaseRange() + unit.getBaseAttack();
                }
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int c = 0; c < weight[i]; c++) {
                weightedUnits.add(i);
            }
        }
    }

    private boolean isCrisisUnit(Unit unit){
        if(unitTargetingMeProbability(unit) <= 0.01){
            return false;
        }
        int range = unit.getRange();
        int distance = me.getKing().getDistance(unit);
        if(distance - range > 3) {
            return false;
        }
        return true;
    }

    public boolean isCrisis(){
        int x = 0;
        List<Unit> badUnits = new ArrayList<>();
        for(Unit enemyUnit : enemyAliveUnits){
            if(isCrisisUnit(enemyUnit)) {
                badUnits.add(enemyUnit);
                x++;
            }
        }
        double kingPower = (double) me.getKing().getAttack() / badUnits.size();
        for(Unit enemyUnit : badUnits) {
            int distance = me.getKing().getDistance(enemyUnit);
            int range = enemyUnit.getRange();
            int kingRange = me.getKing().getRange();
            int time = distance - range;
            if (distance > kingRange){
                if (range >= kingRange) {
                    time = 0;
                }
                else {
                    time -= distance - kingRange;
                }
            }
            if((double) enemyUnit.getHp() <= time * kingPower) x--;
        }
        return x > 0;
    }

    public void doSpell(){

    }

    public void upgrade(){
        if(world.getDamageUpgradeNumber() > 0){
            Unit candidate = me.getUnits().get(0);
            for(Unit unit : me.getUnits()){
                if(unit.getAttack() > candidate.getAttack() && unit.getHp() > candidate.getHp()){
                    candidate = unit;
                }
            }
            System.out.println("upgrade damage: " + candidate.getUnitId());
            world.upgradeUnitDamage(candidate);
        }
        if(world.getRangeUpgradeNumber() > 0){
            Unit candidate = me.getUnits().get(0);
            for(Unit unit : me.getUnits()){
                if(unit.getRange() >= candidate.getRange() && unit.getAttack() > candidate.getAttack() && unit.getHp() > candidate.getHp()){
                    candidate = unit;
                }
            }
            System.out.println("upgrade range: " + candidate.getUnitId());
            world.upgradeUnitRange(candidate);
        }
    }

    public void end(World world, Map<Integer, Integer> scores) {
        System.out.println("game ended");
        System.out.println("my score: " + scores.get(me.getPlayerId()));
    }
}
