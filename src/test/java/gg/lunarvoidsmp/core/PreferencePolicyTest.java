package gg.lunarvoidsmp.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PreferencePolicyTest {
    private final UUID target=UUID.randomUUID(),sender=UUID.randomUUID();

    @Test void spawnProtectionCoversNineChunksAtPositiveAndNegativeBoundaries() {
        var areas=List.of(new SpawnPolicy.Area("world",0,0,true,false));
        assertTrue(SpawnPolicy.blocks("world",-16,-16,false,true,areas));
        assertTrue(SpawnPolicy.blocks("world",31,31,false,true,areas));
        assertFalse(SpawnPolicy.blocks("world",-17,0,false,true,areas));
        assertFalse(SpawnPolicy.blocks("world",32,0,false,true,areas));
        assertEquals(-1,SpawnPolicy.chunk(-1));
        assertEquals(-2,SpawnPolicy.chunk(-17));
    }
    @Test void phantomPreferenceDoesNotBlockOtherMobsOrOtherWorlds() {
        var areas=List.of(new SpawnPolicy.Area("nether",-1,-1,false,true));
        assertTrue(SpawnPolicy.blocks("nether",-1,-1,true,true,areas));
        assertFalse(SpawnPolicy.blocks("nether",-1,-1,false,true,areas));
        assertFalse(SpawnPolicy.blocks("world",-1,-1,true,true,areas));
    }
    @Test void overlappingOffPreferenceWinsButNoProtectedPlayerMeansNoBlock() {
        var areas=List.of(new SpawnPolicy.Area("world",0,0,false,false),new SpawnPolicy.Area("world",1,0,true,false));
        assertTrue(SpawnPolicy.blocks("world",0,0,true,true,areas));
        assertFalse(SpawnPolicy.blocks("world",0,0,false,true,List.of()));
    }
    @Test void spawnPolicyPreservesNonNaturalSpawns() {
        var areas=List.of(new SpawnPolicy.Area("world",0,0,true,true));
        assertFalse(SpawnPolicy.blocks("world",0,0,true,false,areas));
        assertFalse(SpawnPolicy.blocks("world",0,0,false,false,areas));
    }
    @Test void autoTpaAcceptsExistingAndNewOnlineRequestsOnlyInTheIncomingDirection() {
        AutoTpa auto=new AutoTpa(); List<String> accepted=new ArrayList<>();
        var first=new AutoTpa.Request(sender,1,"Bedrock_Player",false,true);
        var offline=new AutoTpa.Request(UUID.randomUUID(),2,"Offline",false,false);
        var here=new AutoTpa.Request(UUID.randomUUID(),3,"OtherDirection",true,true);
        auto.accept(target,true,List.of(first,offline,here),accepted::add);
        auto.accept(target,true,List.of(first,offline,here),accepted::add);
        assertEquals(List.of("Bedrock_Player"),accepted);
        auto.accept(target,true,List.of(first,new AutoTpa.Request(sender,4,"Bedrock_Player",false,true)),accepted::add);
        assertEquals(List.of("Bedrock_Player","Bedrock_Player"),accepted);
    }
    @Test void autoTpaDoesNotConsumeOfflineRequestsAndStopsWhenDisabled() {
        AutoTpa auto=new AutoTpa(); List<String> accepted=new ArrayList<>();
        var offline=new AutoTpa.Request(sender,1,"Player",false,false);
        auto.accept(target,true,List.of(offline),accepted::add);
        auto.accept(target,false,List.of(new AutoTpa.Request(sender,1,"Player",false,true)),accepted::add);
        assertTrue(accepted.isEmpty());
        auto.accept(target,true,List.of(new AutoTpa.Request(sender,1,"Player",false,true)),accepted::add);
        assertEquals(List.of("Player"),accepted);
    }
    @Test void failedAcceptanceIsNotAutomaticallyRetriedAndTargetsRemainIndependent() {
        AutoTpa auto=new AutoTpa(); List<String> accepted=new ArrayList<>();
        var request=new AutoTpa.Request(sender,1,"Player",false,true);
        assertThrows(IllegalStateException.class,()->auto.accept(target,true,List.of(request),name->{ throw new IllegalStateException("Provider interrupted"); }));
        auto.accept(target,true,List.of(request),accepted::add);
        assertTrue(accepted.isEmpty());
        auto.accept(UUID.randomUUID(),true,List.of(request),accepted::add);
        assertEquals(List.of("Player"),accepted);
        auto.forget(target);
        auto.accept(target,true,List.of(request),accepted::add);
        assertEquals(2,accepted.size());
    }
    @Test void notificationSuppressionIsExactRecipientSpecificAndOneUse() {
        NotificationWindow filter=new NotificationWindow();
        filter.prepare(target,List.of("Player has requested to teleport to you.","Use /tpaccept"),100);
        assertFalse(filter.consume(sender,"Use /tpaccept",101));
        assertFalse(filter.consume(target,"Chat: Use /tpaccept",101));
        assertTrue(filter.consume(target,"Use /tpaccept",101));
        assertFalse(filter.consume(target,"Use /tpaccept",101));
        assertTrue(filter.consume(target,"Player has requested to teleport to you.",101));
    }
    @Test void notificationTokensExpireAndLogoutDropsThem() {
        NotificationWindow filter=new NotificationWindow();
        filter.prepare(target,List.of("One","Two"),100);
        assertFalse(filter.consume(target,"One",1100));
        filter.prepare(target,List.of("Two"),1200);
        filter.forget(target);
        assertFalse(filter.consume(target,"Two",1201));
        filter.prepare(target,List.of("Three"),2000);
        filter.expire(3000);
        assertFalse(filter.consume(target,"Three",3000));
    }
    @Test void multipleRequestsCanSuppressTheSamePromptWithoutMutingItPermanently() {
        NotificationWindow filter=new NotificationWindow();
        filter.prepare(target,List.of("Accept"),0);
        filter.prepare(target,List.of("Accept"),1);
        assertTrue(filter.consume(target,"Accept",2));
        assertTrue(filter.consume(target,"Accept",2));
        assertFalse(filter.consume(target,"Accept",2));
        filter.prepare(target,List.of("Accept"),3);
        filter.clear();
        assertFalse(filter.consume(target,"Accept",4));
    }
}
