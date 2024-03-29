package net.corda.node.internal

import net.corda.core.contracts.*
import net.corda.core.cordapp.CordappProvider
import net.corda.core.internal.SerializedStateAndRef
import net.corda.core.node.NetworkParameters
import net.corda.core.node.ServicesForResolution
import net.corda.core.node.services.AttachmentStorage
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.NetworkParametersService
import net.corda.core.node.services.TransactionStorage
import net.corda.core.transactions.ContractUpgradeWireTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.transactions.WireTransaction.Companion.resolveStateRefBinaryComponent

data class ServicesForResolutionImpl(
        override val identityService: IdentityService,
        override val attachments: AttachmentStorage,
        override val cordappProvider: CordappProvider,
        override val networkParametersService: NetworkParametersService,
        private val validatedTransactions: TransactionStorage
) : ServicesForResolution {

    override val networkParameters: NetworkParameters get() = networkParametersService.lookup(networkParametersService.currentHash) ?:
            throw IllegalArgumentException("No current parameters in network parameters storage")

    @Throws(TransactionResolutionException::class)
    override fun loadState(stateRef: StateRef): TransactionState<*> {
        val stx = validatedTransactions.getTransaction(stateRef.txhash) ?: throw TransactionResolutionException(stateRef.txhash)
        return stx.resolveBaseTransaction(this).outputs[stateRef.index]
    }

    @Throws(TransactionResolutionException::class)
    override fun loadStates(stateRefs: Set<StateRef>): Set<StateAndRef<ContractState>> {
        return stateRefs.groupBy { it.txhash }.flatMap {
            val stx = validatedTransactions.getTransaction(it.key) ?: throw TransactionResolutionException(it.key)
            val baseTx = stx.resolveBaseTransaction(this)
            it.value.map { StateAndRef(baseTx.outputs[it.index], it) }
        }.toSet()
    }

    @Throws(TransactionResolutionException::class, AttachmentResolutionException::class)
    override fun loadContractAttachment(stateRef: StateRef, forContractClassName: ContractClassName?): Attachment {
        val coreTransaction = validatedTransactions.getTransaction(stateRef.txhash)?.coreTransaction
                ?: throw TransactionResolutionException(stateRef.txhash)
        when (coreTransaction) {
            is WireTransaction -> {
                val transactionState = coreTransaction.outRef<ContractState>(stateRef.index).state
                for (attachmentId in coreTransaction.attachments) {
                    val attachment = attachments.openAttachment(attachmentId)
                    if (attachment is ContractAttachment && (forContractClassName ?: transactionState.contract) in attachment.allContracts) {
                        return attachment
                    }
                }
                throw AttachmentResolutionException(stateRef.txhash)
            }
            is ContractUpgradeWireTransaction -> {
                return attachments.openAttachment(coreTransaction.upgradedContractAttachmentId) ?: throw AttachmentResolutionException(stateRef.txhash)
            }
            is NotaryChangeWireTransaction -> {
                val transactionState = SerializedStateAndRef(resolveStateRefBinaryComponent(stateRef, this)!!, stateRef).toStateAndRef().state
                //TODO check only one (or until one is resolved successfully), max recursive invocations check?
                return coreTransaction.inputs.map { loadContractAttachment(it, transactionState.contract) }.firstOrNull() ?: throw AttachmentResolutionException(stateRef.txhash)
            }
            else -> throw UnsupportedOperationException("Attempting to resolve attachment ${stateRef.index} of a ${coreTransaction.javaClass} transaction. This is not supported.")
        }
    }
}
