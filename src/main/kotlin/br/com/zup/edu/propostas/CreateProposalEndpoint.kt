package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.CreateProposalResponse
import br.com.zup.edu.PropostasGrpcServiceGrpc
import br.com.zup.edu.shared.grpc.ErrorHandler
import com.google.protobuf.Timestamp
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional

@ErrorHandler
@Singleton
open class CreateProposalEndpoint(@Inject val repository: ProposalRespository) : PropostasGrpcServiceGrpc.PropostasGrpcServiceImplBase() {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    open override fun create(request: CreateProposalRequest, responseObserver: StreamObserver<CreateProposalResponse>) {

        LOGGER.info("New Request: $request")

        if (repository.existsByDocument(request.document)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                                .withDescription("proposal already exists")
                                .asRuntimeException())
            return // it's important to stop the flow
        }

        val proposal = repository.save(request.toModel())

        responseObserver.onNext(CreateProposalResponse.newBuilder()
                                        .setId(proposal.id.toString())
                                        .setCreatedAt(proposal.createdAt.toGrpcTimestamp())
                                        .build())
        responseObserver.onCompleted()
    }

}

/**
 * Extension methods
 */

fun CreateProposalRequest.toModel(): Proposal {
    return Proposal(
        document = document,
        email = email,
        name = name,
        address = address,
        salary = BigDecimal(salary)
    )
}

fun LocalDateTime.toGrpcTimestamp(): Timestamp {
    val instant = this.atZone(ZoneId.of("UTC")).toInstant()
    return Timestamp.newBuilder()
                    .setSeconds(instant.epochSecond)
                    .setNanos(instant.nano)
                    .build()
}