package elucent.eidolon.common.entity;

import elucent.eidolon.capability.IReputation;
import elucent.eidolon.client.particle.Particles;
import elucent.eidolon.common.deity.Deities;
import elucent.eidolon.network.MagicBurstEffectPacket;
import elucent.eidolon.network.Networking;
import elucent.eidolon.registries.EidolonEntities;
import elucent.eidolon.registries.EidolonParticles;
import elucent.eidolon.util.ColorUtil;
import elucent.eidolon.util.EntityUtil;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

public class NecromancerEntity extends SpellcasterIllager {
    public NecromancerEntity(EntityType<? extends SpellcasterIllager> type, Level worldIn) {
        super(type, worldIn);
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    boolean hack = false;

    @Override
    public boolean isCastingSpell() {
        return (!level.isClientSide || !hack) && super.isCastingSpell();
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity pEntity) {
        if (pEntity == this) {
            return true;
        } else if (super.isAlliedTo(pEntity)) {
            return true;
        } else if (pEntity instanceof LivingEntity && ((LivingEntity) pEntity).getMobType() == MobType.ILLAGER) {
            return this.getTeam() == null && pEntity.getTeam() == null;
        } else {
            return false;
        }
    }

    @Override
    public void tick() {
        hack = true; // Used to avoid the default spell particles from SpellcastingIllagerEntity
        super.tick();
        hack = false;
        if (this.level.isClientSide && this.isCastingSpell()) {
            IllagerSpell spelltype = getCurrentSpell();
            float f = this.yBodyRot * ((float) Math.PI / 180F) + Mth.cos((float) this.tickCount * 0.6662F) * 0.25F;
            float f1 = Mth.cos(f);
            float f2 = Mth.sin(f);
            if (spelltype == IllagerSpell.FANGS) {
                Particles.create(EidolonParticles.SPARKLE_PARTICLE)
                        .setColor(1, 0.3125f, 0.375f, 0.75f, 0.375f, 1)
                        .randomVelocity(0.05f).randomOffset(0.025f)
                        .setScale(0.25f, 0.125f).setAlpha(0.25f, 0)
                        .setSpin(0.4f)
                        .spawn(level, getX() + f1 * 0.875, getY() + 2.0, getZ() + f2 * 0.875)
                        .spawn(level, getX() - f1 * 0.875, getY() + 2.0, getZ() - f2 * 0.875);
            } else if (spelltype == IllagerSpell.SUMMON_VEX) {
                Particles.create(EidolonParticles.WISP_PARTICLE)
                        .setColor(0.75f, 1, 1, 0.125f, 0.125f, 0.875f)
                        .randomVelocity(0.05f).randomOffset(0.025f)
                        .setScale(0.25f, 0.125f).setAlpha(0.25f, 0)
                        .spawn(level, getX() + f1 * 0.875, getY() + 2.0, getZ() + f2 * 0.875)
                        .spawn(level, getX() - f1 * 0.875, getY() + 2.0, getZ() - f2 * 0.875);
            }
        }
    }

    @Override
    public boolean isInvertedHealAndHarm() {
        return true;
    }

    class AttackSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private AttackSpellGoal() {
        }

        public boolean canUse() {
            LivingEntity livingentity = NecromancerEntity.this.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                if (NecromancerEntity.this.isCastingSpell()) {
                    return false;
                } else {
                    return NecromancerEntity.this.tickCount >= this.nextAttackTickCount;
                }
            }
            return false;
        }

        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 80;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_SUMMON;
        }

        @Override
        protected @NotNull IllagerSpell getSpell() {
            return IllagerSpell.FANGS;
        }

        @Override
        protected void performSpellCasting() {
            LivingEntity target = NecromancerEntity.this.getTarget();
            Vec3 diff = target.position().subtract(NecromancerEntity.this.position());
            Vec3 norm = diff.normalize();
            if (!level.isClientSide) {
                for (int i = 0; i < 3; i++) {
                    NecromancerSpellEntity spell = new NecromancerSpellEntity(level, getX(), getEyeY(), getZ(), norm.x + random.nextFloat() * 0.1 - 0.05, norm.y + 0.04 * diff.length() / 2 + random.nextFloat() * 0.1 - 0.05, norm.z + random.nextFloat() * 0.1 - 0.05, i * 5);
                    spell.setOwner(NecromancerEntity.this);
                    level.addFreshEntity(spell);
                }
            }
        }
    }

    class SummonSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private SummonSpellGoal() {
        }

        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 200;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_SUMMON;
        }

        @Override
        protected @NotNull IllagerSpell getSpell() {
            return IllagerSpell.SUMMON_VEX;
        }

        @Override
        protected void performSpellCasting() {
            if (!level.isClientSide) {
                EntityType<?> type = random.nextBoolean() ? EntityType.SKELETON : EntityType.ZOMBIE;
                if (NecromancerEntity.this.getHealth() < NecromancerEntity.this.getMaxHealth() / 2) {
                    type = random.nextBoolean() ? EidolonEntities.GIANT_SKEL.get() : EidolonEntities.ZOMBIE_BRUTE.get();
                }
                Holder<Biome> biomeKey = level.getBiome(blockPosition());
                for (int i = 0; i < random.nextInt(5); i++) {
                    if (type == EntityType.SKELETON && biomeKey.is(Tags.Biomes.IS_COLD))
                        type = EntityType.STRAY;
                    if (type == EntityType.SKELETON && biomeKey.is(BiomeTags.IS_NETHER))
                        type = EntityType.WITHER_SKELETON;
                    if (type == EntityType.ZOMBIE && biomeKey.is(Tags.Biomes.IS_SANDY))
                        type = EntityType.HUSK;
                    if (type == EntityType.ZOMBIE && biomeKey.is(Tags.Biomes.IS_WET))
                        type = EntityType.DROWNED;

                    var entity = type.create(level);
                    if (!(entity instanceof Monster thrall)) return;
                    thrall.setPos(getX(), getY(), getZ());
                    level.addFreshEntity(entity);
                    thrall.setTarget(getTarget());
                    EntityUtil.enthrall(NecromancerEntity.this, thrall);
                    thrall.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, false));
                    Networking.sendToTracking(level, blockPosition(), new MagicBurstEffectPacket(getX(), getY() + 1, getZ(), ColorUtil.packColor(255, 181, 255, 255), ColorUtil.packColor(255, 28, 31, 212)));
                }
            }
        }
    }

    class CastingSpellGoal extends SpellcasterIllager.SpellcasterCastingSpellGoal {
        private CastingSpellGoal() {
        }

        public void tick() {
            if (NecromancerEntity.this.getTarget() != null) {
                NecromancerEntity.this.getLookControl().setLookAt(NecromancerEntity.this.getTarget(), (float) NecromancerEntity.this.getMaxHeadYRot(), (float) NecromancerEntity.this.getMaxHeadXRot());
            }
        }
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastingSpellGoal());
        this.goalSelector.addGoal(5, new AttackSpellGoal());
        this.goalSelector.addGoal(4, new SummonSpellGoal());
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Raider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, (new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (e) -> e.getCommandSenderWorld().getCapability(IReputation.INSTANCE).isPresent() && e.getCommandSenderWorld().getCapability(IReputation.INSTANCE).resolve().get().getReputation((Player) e, Deities.DARK_DEITY.getId()) >= 50)).setUnseenMemoryTicks(300));
        //this.targetSelector.addGoal(2, (new NearestAttackableTargetGoal<>(this, Player.class, true)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
    }

    protected @NotNull SoundEvent getCastingSoundEvent() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    public static AttributeSupplier createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 12.0D)
                .build();
    }

    @Override
    public void applyRaidBuffs(int wave, boolean p_213660_2_) {
    }

    @Override
    public @NotNull SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }
}
