package app.tab.store.data.repository

import app.tab.store.core.firebase.firestoreResult
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.firebase.toUserMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toAddress
import app.tab.store.domain.model.Address
import app.tab.store.domain.repository.AddressRepository
import app.tab.store.domain.repository.DataResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class AddressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : AddressRepository {

    private val uid: String? get() = auth.currentUser?.uid

    private fun col() = uid?.let { firestore.collection(FirestorePaths.addressItems(it)) }

    override fun observeAddresses(): Flow<List<Address>> {
        val ref = col() ?: return flowOf(emptyList())
        return ref.snapshotFlow().map { snap ->
            snap.documents.map { it.toAddress() }
                .sortedWith(compareByDescending<Address> { it.isDefault }.thenBy { it.label })
        }.orFallback(emptyList())
    }

    override suspend fun upsert(address: Address): DataResult<String> {
        val uid = uid ?: return DataResult.Failure("Sign in to save addresses.")
        return try {
            val ref = col() ?: return DataResult.Failure("Sign in to save addresses.")
            val docRef = if (address.id.isBlank()) ref.document() else ref.document(address.id)
            val payload = mapOf(
                "label" to address.label,
                "fullName" to address.fullName,
                "phone" to address.phone,
                "line1" to address.line1,
                "line2" to address.line2,
                "city" to address.city,
                "state" to address.state,
                "pincode" to address.pincode,
                "isDefault" to address.isDefault,
            )
            if (address.isDefault) {
                ref.get().await().documents.forEach { doc ->
                    if (doc.getBoolean("isDefault") == true) {
                        doc.reference.update("isDefault", false).await()
                    }
                }
            }
            docRef.set(payload, SetOptions.merge()).await()
            DataResult.Success(docRef.id)
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }
    }

    override suspend fun delete(addressId: String): DataResult<Unit> {
        val ref = col() ?: return DataResult.Failure("Sign in to manage addresses.")
        return firestoreResult { ref.document(addressId).delete().await() }
    }

    override suspend fun setDefault(addressId: String): DataResult<Unit> {
        val ref = col() ?: return DataResult.Failure("Sign in to manage addresses.")
        return firestoreResult {
            ref.get().await().documents.forEach { doc ->
                doc.reference.update("isDefault", doc.id == addressId).await()
            }
        }
    }
}
