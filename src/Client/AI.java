package Client;

import Client.Model.*;

import java.util.*;
import java.util.Map;

// shab be kheir

/**
 * You must put your code in this class {@link AI}.
 * This class has {@link #pick}, to choose units before the start of the game;
 * {@link #turn}, to do orders while game is running;
 * {@link #end}, to process after the end of the game;
 */

public class AI {
    private Random random = new Random();
    private World world, lastWorld;

    public void pick(World world) {
        System.out.println("pick started");

        List<BaseUnit> myHand = new ArrayList<>();
        myHand.add(world.getBaseUnitById(0));
        myHand.add(world.getBaseUnitById(1));
        myHand.add(world.getBaseUnitById(2));
        myHand.add(world.getBaseUnitById(6));
        myHand.add(world.getBaseUnitById(7));

        // picking the chosen hand - rest of the hand will automatically be filled with random baseUnits
        world.chooseHand(myHand);
    }

    private Player me, friend, en1, en2;
    private Map<Integer, List<Integer>> enemyUnitsPaths = new HashMap<>();

    private void findEnemyUnitsPaths(Player first, Player second){
        for(var unit : first.getUnits()){
            if(unit.getHp() > 1){
                List<Integer> PathIds = new ArrayList<>();
                if(enemyUnitsPaths.containsKey(unit.getUnitId())){
                    for(var path : world.getPathsCrossingCell(unit.getCell())){
                        if(enemyUnitsPaths.get(unit.getUnitId()).contains(path.getId())){
                            PathIds.add(path.getId());
                        }
                    }
                    if(PathIds.size() > 0){
                        enemyUnitsPaths.replace(unit.getUnitId(), PathIds);
                    }
                }
                else{
                    for(var path : world.getPathsCrossingCell(unit.getCell())){
                        PathIds.add(path.getId());
                    }
                    if(PathIds.size() == 0){
                        for(var path : second.getPathsFromPlayer()){
                            PathIds.add(path.getId());
                        }
                    }
                    enemyUnitsPaths.put(unit.getUnitId(), PathIds);
                }
            }
        }
    }

    public void turn(World world) {
        this.world = world;

        me = world.getMe();
        friend = world.getFriend();
        en1 = world.getFirstEnemy();
        en2 = world.getSecondEnemy();
        findEnemyUnitsPaths(en1, en2);
        findEnemyUnitsPaths(en2, en1);

        lastWorld = world;
    }

    public void end(World world, Map<Integer, Integer> scores) {
        System.out.println("end started");
        System.out.println("My score: " + scores.get(world.getMe().getPlayerId()));
    }
}
