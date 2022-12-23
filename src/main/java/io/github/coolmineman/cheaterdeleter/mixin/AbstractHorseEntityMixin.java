package io.github.coolmineman.cheaterdeleter.mixin;

import org.spongepowered.asm.mixin.Mixin;

import io.github.coolmineman.cheaterdeleter.objects.entity.CDAbstractHorseEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin implements CDAbstractHorseEntity {
    
}
