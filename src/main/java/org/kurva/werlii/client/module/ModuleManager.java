package org.kurva.werlii.client.module;

import net.minecraft.client.MinecraftClient;
import org.kurva.werlii.client.module.combat.*;
import org.kurva.werlii.client.module.exploit.*;
import org.kurva.werlii.client.module.movement.*;
import org.kurva.werlii.client.module.render.*;
import org.kurva.werlii.client.module.utility.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
private final List<Module> modules = new ArrayList<>();

public ModuleManager() {
  // Combat
  modules.add(new CrystalAura());
  modules.add(new AutoCrystal());
  modules.add(new CrystalOptimizer());
  modules.add(new AirAnchor());
  modules.add(new AutoTotem());
  modules.add(new KillAura());
  modules.add(new Surround());
  modules.add(new AntiKnockback());
  modules.add(new Criticals());
  modules.add(new Reach());
  modules.add(new AutoDoubleHand());
  modules.add(new AutoInventoryTotem());
  modules.add(new DoubleAnchor());
  modules.add(new Hitboxes());
  modules.add(new AnchorMacro());
  modules.add(new AutoHitCrystal());
  modules.add(new ShieldBreaker());
  modules.add(new AutoTrap());
  modules.add(new HoleFiller());
  modules.add(new AutoCity());
  modules.add(new AutoGapple());
  modules.add(new FastPlace());
  modules.add(new BurrowDetector());
  modules.add(new BedAura());
  
  // Movement
  modules.add(new Speed());
  modules.add(new Step());
  modules.add(new ElytraFlight());
  modules.add(new Jesus());
  modules.add(new Scaffold());
  
  // Render
  modules.add(new HUD());
  modules.add(new ClickGUI());
  modules.add(new ESP());
  modules.add(new BlockESP());
  modules.add(new Tracers());
  modules.add(new XRay());
  modules.add(new Trajectories());
  modules.add(new NoRender());
  modules.add(new CustomFont()); // Add the new CustomFont module
  
  // Exploit
  modules.add(new PacketMine());
  modules.add(new NoFall());
  modules.add(new NoSlow());
  modules.add(new Timer());
  modules.add(new XCarry());
  modules.add(new Freecam());
  modules.add(new Phase());
  modules.add(new AntiHunger());
  
  // Utility
  modules.add(new AutoEXP());
  modules.add(new AutoTool());
  modules.add(new PanicMode());
  modules.add(new StreamMode());
  modules.add(new AutoFish());
  modules.add(new MiddleClickFriend());
  modules.add(new AutoRespawn());
  modules.add(new ClickPearl());
}

public List<Module> getModules() {
    return modules;
}

public Module getModuleByName(String name) {
    return modules.stream()
            .filter(module -> module.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
}

public List<Module> getModulesByCategory(Module.Category category) {
    return modules.stream()
            .filter(module -> module.getCategory() == category)
            .collect(Collectors.toList());
}

public List<Module> getEnabledModules() {
    return modules.stream()
            .filter(Module::isEnabled)
            .collect(Collectors.toList());
}

public void onTick(MinecraftClient client) {
    for (Module module : modules) {
        if (module.isEnabled()) {
            module.onTick();
        }
    }
}

public void onRender(float tickDelta) {
    for (Module module : modules) {
        if (module.isEnabled()) {
            module.onRender(tickDelta);
        }
    }
}

public void registerKeybindings() {
    for (Module module : modules) {
        if (module.getKeyBinding() != null) {
            // In a real implementation, you would register the keybinding with Minecraft
            // This is a simplified version
        }
    }
}

public void toggleModule(String name) {
    Module module = getModuleByName(name);
    if (module != null) {
        module.toggle();
    }
}

public void enableModule(String name) {
    Module module = getModuleByName(name);
    if (module != null && !module.isEnabled()) {
        module.setEnabled(true);
    }
}

public void disableModule(String name) {
    Module module = getModuleByName(name);
    if (module != null && module.isEnabled()) {
        module.setEnabled(false);
    }
}

public void disableAll() {
    for (Module module : modules) {
        if (module.isEnabled()) {
            module.setEnabled(false);
        }
    }
}

public void enableAll() {
    for (Module module : modules) {
        if (!module.isEnabled()) {
            module.setEnabled(true);
        }
    }
}

public void handleKeyPress(int keyCode) {
    for (Module module : modules) {
        if (module.getKeyCode() == keyCode) {
            module.toggle();
        }
    }
}
}

