package app.revanced.patches.music.layout.zenmode.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.*
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.music.misc.integrations.patch.MusicIntegrationsPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.music.layout.zenmode.fingerprints.ZenModeFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.fingerprints.MiniplayerColorParentFingerprint
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("enable-zen-mode")
@Description("Adds a grey tint to the video player to reduce eye strain.")
@DependsOn([MusicIntegrationsPatch::class, MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class ZenModePatch : BytecodePatch(
    listOf(
        ZenModeFingerprint, MiniplayerColorParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        val miniplayerColorParentResult = MiniplayerColorParentFingerprint.result!!

        ZenModeFingerprint.resolve(context, miniplayerColorParentResult.classDef)
        val ZenModeResult = ZenModeFingerprint.result!!
        val ZenModeMethod = ZenModeResult.mutableMethod
        val ZenModeInstructions = ZenModeMethod.implementation!!.instructions

        val startIndex = ZenModeResult.scanResult.patternScanResult!!.startIndex

        val firstRegister = (ZenModeMethod.instruction(startIndex) as OneRegisterInstruction).registerA
        val secondRegister = (ZenModeMethod.instruction(startIndex + 2) as OneRegisterInstruction).registerA
        val dummyRegister = secondRegister + 1

        val referenceIndex = ZenModeResult.scanResult.patternScanResult!!.endIndex + 1
        val Ref = (ZenModeInstructions.elementAt(referenceIndex) as ReferenceInstruction).reference as FieldReference

        val insertIndex = referenceIndex + 1

        ZenModeMethod.addInstructions(
            insertIndex, """
            invoke-static {}, $MUSIC_SETTINGS_PATH->enableZenMode()Z
            move-result v$dummyRegister
            if-eqz v$dummyRegister, :off
            const v$dummyRegister, -0xfcfcfd
            if-ne v$firstRegister, v$dummyRegister, :off
            const v$firstRegister, -0xbfbfc0
            const v$secondRegister, -0xbfbfc0
            :off
            sget-object v0, ${Ref.type}->${Ref.name}:${Ref.type}
        """
        )

        ZenModeMethod.removeInstruction(insertIndex - 1)

        return PatchResultSuccess()
    }
}