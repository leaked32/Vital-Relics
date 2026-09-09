import com.example.vitalrelics.common.ArrowDurability;
import com.example.vitalrelics.common.MiningSkills;
import com.example.vitalrelics.common.MyEvents;
import com.example.vitalrelics.common.MyRuntime;
import com.example.vitalrelics.common.MySpellSystem;
import com.example.vitalrelics.common.platform.*;
import com.example.vitalrelics.common.relics.Relic;
import java.lang.reflect.Proxy;
import java.util.*;

/** Dependency-free behavioral tests. Run with the compiled common classes on the classpath. */
public class RareRelicRulesTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static final class World implements MiningSkills.Context {
        final Set<MiningSkills.Pos> logs = new HashSet<>(), blocks = new HashSet<>(), broken = new HashSet<>();
        final Set<MiningSkills.Pos> protectedBlocks = new HashSet<>();
        int uses = 10000;
        public boolean valid() { return uses > 0; }
        public boolean isLog(MiningSkills.Pos p) { return logs.contains(p); }
        public boolean breakBlock(MiningSkills.Pos p) {
            if (!valid() || protectedBlocks.contains(p) || !blocks.remove(p)) return false;
            broken.add(p); uses--; return true;
        }
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0F;
        if (type == int.class) return 0;
        if (type == List.class) return List.of();
        return null;
    }
    private static MyLivingEntity entity(double x, List<MyLivingEntity> neighbors, Set<MyLivingEntity> hostile, List<String> effects) {
        return (MyLivingEntity) Proxy.newProxyInstance(RareRelicRulesTest.class.getClassLoader(), new Class[]{MyLivingEntity.class}, (self,method,args) -> switch(method.getName()) {
            case "x" -> x;
            case "is" -> self == args[0];
            case "equals" -> self == args[0];
            case "hashCode" -> System.identityHashCode(self);
            case "livingEntitiesInRange" -> neighbors;
            case "isHostile" -> hostile.contains(args[0]);
            case "addEffect" -> { effects.add(args[0]+":"+args[1]+":"+args[2]); yield null; }
            default -> defaultValue(method.getReturnType());
        });
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ArrowDurability arrow = new ArrowDurability(4);
        check(!arrow.canBreak(4), "Equal hardness must stop the arrow");
        check(!arrow.canBreak(-1), "Unbreakable blocks must stop the arrow");
        check(!arrow.canBreak(Double.NaN), "Invalid hardness must be rejected");
        check(arrow.canBreak(3), "Lower hardness should be breakable");
        arrow.spend(3);
        check(arrow.canBreak(0), "Durability must not decay over time");
        for (double invalid : new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            try { new ArrowDurability(invalid); throw new AssertionError("Accepted invalid durability"); }
            catch (IllegalArgumentException expected) {}
        }
        MiningSkills.Pos origin = new MiningSkills.Pos(0,0,0);
        World area = new World();
        for (int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++) area.blocks.add(new MiningSkills.Pos(x,y,z));
        area.protectedBlocks.add(new MiningSkills.Pos(1,0,0));
        MiningSkills.apply(area,origin,false,1);
        check(area.broken.size()==5, "Radius 1 should break six face neighbors minus one protected block");
        check(area.blocks.contains(origin), "Never re-break the original block");
        World tree = new World();
        for(int y=1;y<=8;y++) { var p=new MiningSkills.Pos(0,y,0);tree.logs.add(p);tree.blocks.add(p); }
        var isolated=new MiningSkills.Pos(4,4,0);tree.logs.add(isolated);tree.blocks.add(isolated);
        MiningSkills.apply(tree,origin,true,0);
        check(tree.broken.size()==8 && tree.blocks.contains(isolated), "Felling should follow connected logs only");
        World exhausted = new World();exhausted.uses=2;
        for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++) exhausted.blocks.add(new MiningSkills.Pos(x,y,z));
        MiningSkills.apply(exhausted,origin,false,2);
        check(exhausted.broken.size()==2, "Stop immediately when the original tool context expires");
        World large = new World();
        for(int x=-9;x<=9;x++) for(int y=-9;y<=9;y++) for(int z=-9;z<=9;z++) large.blocks.add(new MiningSkills.Pos(x,y,z));
        MiningSkills.apply(large,origin,false,100000);
        check(large.broken.size()==512, "Enforce the work budget");
        check(large.broken.stream().allMatch(p->p.distanceSquared(origin)<=64), "Enforce the radius cap");
        List<String> applied = new ArrayList<>();
        var enemy = entity(10,List.of(),Set.of(),applied);
        var outside = entity(10.01,List.of(),Set.of(),applied);
        var ally = entity(1,List.of(),Set.of(),applied);
        var caster = entity(0,List.of(enemy,outside,ally),Set.of(enemy,outside),new ArrayList<>());
        MyLivingEntity[] pointed = {null};
        MyLivingEntity[] strike = new MyLivingEntity[2];
        double[] parameters = new double[2];
        MyRuntime.initialize((MyRuntimeUtils) Proxy.newProxyInstance(RareRelicRulesTest.class.getClassLoader(),new Class[]{MyRuntimeUtils.class},(self,method,arguments)-> {
            if (method.getName().equals("pointedLivingEntity")) return pointed[0];
            if (method.getName().equals("forceAttack")) { strike[0]=(MyLivingEntity)arguments[0];strike[1]=(MyLivingEntity)arguments[1];return true; }
            if (method.getName().equals("launchBorebolt")) { for(int i=0;i<2;i++) parameters[i]=(double)arguments[i+1];return true; }
            return defaultValue(method.getReturnType());
        }));
        Relic frost = new Relic();frost.passive_skills.put(Relic.PASSIVE_SKILL_SLOWING_AURA,10.0);
        MyEvents.onLivingEntityTick(caster,19,List.of(frost));
        check(applied.isEmpty(),"Aura must wait for its 20-tick cadence");
        MyEvents.onLivingEntityTick(caster,20,List.of(frost));
        check(applied.equals(List.of("slowness:60:2")),"Only hostile targets inside the sphere receive Slowness III for 60 ticks");
        var handlersField = MySpellSystem.class.getDeclaredField("handlers");handlersField.setAccessible(true);
        var handlers = (Map<String,MySpellSystem.Handler>)handlersField.get(MySpellSystem.INSTANCE);
        Relic.Spells.Info info = new Relic.Spells.Info();info.parameters.put("speed",3.0);info.parameters.put("durability",19.0);
        check(handlers.get("borebolt").activate(caster,info),"Valid borebolt spell should dispatch");
        check(Arrays.equals(parameters,new double[]{3,19}),"Both arrow configuration values must reach the adapter");
        info.parameters.put("speed",Double.NaN);
        check(!handlers.get("borebolt").activate(caster,info),"Reject invalid speed before spawning");
        pointed[0] = entity(2,List.of(enemy,caster,ally),Set.of(),new ArrayList<>());
        check(handlers.get("compel_attack").activate(caster,new Relic.Spells.Info()),"Forced attack must dispatch immediately");
        check(strike[0]==pointed[0] && strike[1]==ally,"Choose the nearest living entity without an allegiance filter");
        boolean[] arrowState = new boolean[2];
        UUID arrowId = UUID.randomUUID();
        MyAbstractArrow projectile = (MyAbstractArrow) Proxy.newProxyInstance(
                RareRelicRulesTest.class.getClassLoader(),new Class[]{MyAbstractArrow.class},
                (self,method,arguments)->switch(method.getName()) {
                    case "uuid" -> arrowId;
                    case "velocityX", "velocityY", "velocityZ", "baseDamage" -> 1.0;
                    case "setNoGravity" -> { arrowState[0]=(boolean)arguments[0];yield null; }
                    case "discard" -> { arrowState[1]=true;yield null; }
                    default -> defaultValue(method.getReturnType());
                });
        Relic emblem = new Relic();
        emblem.passive_skills.put(Relic.PASSIVE_SKILL_GRAVITYLESS_ARROWS,1.0);
        MyEvents.onArrowShot(projectile,caster,List.of(emblem),100);
        check(arrowState[0] && !arrowState[1],"A positive passive level must disable arrow gravity immediately");
        for(int tick=101;tick<120;tick++) Scheduler.INSTANCE().serverTick(tick);
        check(!arrowState[1],"A level-one gravityless arrow must survive its first 19 ticks");
        Scheduler.INSTANCE().serverTick(120);
        check(arrowState[1],"A level-one gravityless arrow must disappear after 20 ticks");
        System.out.println("PASS: durability, mining, aura, spells, forced attack and gravityless arrow lifetime");
    }
}
