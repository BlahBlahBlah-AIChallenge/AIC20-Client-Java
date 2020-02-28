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
    private Path selectedPath;

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

        selectedPath = world.getMe().getPathsFromPlayer().get(0);
        for(var path : world.getMe().getPathsFromPlayer()){
            if(path.getCells().size() < selectedPath.getCells().size()){
                selectedPath = path;
            }
        }

    }

    private Player me, friend, en1, en2;
    private Client.Model.Map map;

    private List<Integer> myPaths = new ArrayList<>(), friendPaths = new ArrayList<>();
    private Map<Integer, List<Integer>> enemyUnitsPaths = new HashMap<>();
    private Map<Integer, Set<Integer>> enemyUnitsTargetKing = new HashMap<>();

    private int[] unitWeight = new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1};

    private void findEnemyUnitsPaths(Player first, Player second){
        for(var unit : first.getUnits()){
            if(unit.getHp() > 1){
                List<Integer> PathIds = new ArrayList<>();
                Set<Integer> TargetKingIds = new HashSet<>();
                if(enemyUnitsPaths.containsKey(unit.getUnitId())){
                    for(var path : world.getPathsCrossingCell(unit.getCell())){
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
                    for(var path : world.getPathsCrossingCell(unit.getCell())){
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
                        for(var path : second.getPathsFromPlayer()){
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
    }

    public void turn(World world) {
        this.world = world;

        me = world.getMe();
        friend = world.getFriend();
        en1 = world.getFirstEnemy();
        en2 = world.getSecondEnemy();
        map = world.getMap();
        for(var path : me.getPathsFromPlayer()){
            myPaths.add(path.getId());
        }
        for(var path : friend.getPathsFromPlayer()){
            friendPaths.add(path.getId());
        }
        findEnemyUnitsPaths(en1, en2);
        findEnemyUnitsPaths(en2, en1);

        lastWorld = world;
    }

    public void parse(){

    }

    public void clacAttackWeights(){

    }

    public void clacDefencekWeights(){

    }

    public Boolean isCrisis(){
        return false;
    }

    public void attack(){

    }

    public void defence(){

    }

    public void doSpell(){

    }

    public void end(World world, Map<Integer, Integer> scores) {
        System.out.println("end started");
        System.out.println("My score: " + scores.get(world.getMe().getPlayerId()));
    }
}
