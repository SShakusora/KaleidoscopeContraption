package com.sshakusora.kaleidoscope_contraption.mixin.accessor;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ClientContraption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(Contraption.class)
public interface ContraptionClientAccessor {
    @Accessor("clientContraption")
    AtomicReference<ClientContraption> getClientContraptionReference();

    @Invoker("createClientContraption")
    ClientContraption invokeCreateClientContraption();
}
