package app.revanced.patches.shared.patch.litho

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.litho.EmptyComponentBuilderFingerprint
import app.revanced.patches.shared.fingerprints.litho.IdentifierFingerprint
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import kotlin.properties.Delegates

object ComponentParserPatch : BytecodePatch(
    setOf(
        EmptyComponentBuilderFingerprint,
        IdentifierFingerprint
    )
) {
    private lateinit var emptyComponentLabel: String
    internal lateinit var insertMethod: MutableMethod

    private var emptyComponentIndex by Delegates.notNull<Int>()
    private var insertIndex by Delegates.notNull<Int>()

    private var identifierRegister by Delegates.notNull<Int>()
    private var objectRegister by Delegates.notNull<Int>()
    private var stringBuilderRegister by Delegates.notNull<Int>()

    internal fun generalHook(descriptor: String) {
        insertMethod.apply {
            addInstructionsWithLabels(
                insertIndex,
                """
                        invoke-static {v$stringBuilderRegister, v$identifierRegister, v$objectRegister}, $descriptor(Ljava/lang/StringBuilder;Ljava/lang/String;Ljava/lang/Object;)Z
                        move-result v$stringBuilderRegister
                        if-eqz v$stringBuilderRegister, :filter
                        """ + emptyComponentLabel,
                ExternalLabel("filter", getInstruction(insertIndex))
            )
        }
    }

    internal fun pathBuilderHook(descriptor: String) {
        insertMethod.apply {
            addInstructionsWithLabels(
                insertIndex,
                """
                        invoke-static {v$stringBuilderRegister}, $descriptor(Ljava/lang/StringBuilder;)Z
                        move-result v$stringBuilderRegister
                        if-eqz v$stringBuilderRegister, :filter
                        """ + emptyComponentLabel,
                ExternalLabel("filter", getInstruction(insertIndex))
            )
        }
    }

    override fun execute(context: BytecodeContext) {

        /**
         * Shared fingerprint
         */
        EmptyComponentBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                insertMethod = this
                emptyComponentIndex = it.scanResult.patternScanResult!!.startIndex + 1

                val builderMethodDescriptor =
                    getInstruction<ReferenceInstruction>(emptyComponentIndex).reference
                val emptyComponentFieldDescriptor =
                    getInstruction<ReferenceInstruction>(emptyComponentIndex + 2).reference

                emptyComponentLabel = """
                        move-object/from16 v0, p1
                        invoke-static {v0}, $builderMethodDescriptor
                        move-result-object v0
                        iget-object v0, v0, $emptyComponentFieldDescriptor
                        return-object v0
                        """

                val stringBuilderIndex =
                    implementation!!.instructions.indexOfFirst { instruction ->
                        val fieldReference =
                            (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        fieldReference?.let { reference -> reference.type == "Ljava/lang/StringBuilder;" } == true
                    }

                stringBuilderRegister =
                    getInstruction<TwoRegisterInstruction>(stringBuilderIndex).registerA

                insertIndex = stringBuilderIndex + 1
            }
        } ?: throw EmptyComponentBuilderFingerprint.exception

        /**
         * Only used in YouTube
         */
        IdentifierFingerprint.result?.let {
            it.mutableMethod.apply {
                val identifierIndex = it.scanResult.patternScanResult!!.startIndex
                val objectIndex = it.scanResult.patternScanResult!!.endIndex + 1

                identifierRegister =
                    getInstruction<OneRegisterInstruction>(identifierIndex).registerA
                objectRegister = getInstruction<BuilderInstruction35c>(objectIndex).registerC
            }
        }

    }
}