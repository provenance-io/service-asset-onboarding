package io.provenance.asset

import com.fasterxml.jackson.databind.node.ObjectNode
import io.provenance.client.protobuf.extensions.toAny
import org.junit.jupiter.api.Test
import io.provenance.asset.extensions.randomUuid
import io.provenance.asset.extensions.toJsonString
import io.provenance.asset.extensions.writeFile
import tech.figure.asset.v1beta1.AssetOuterClassBuilders
import tech.figure.loan.v1beta1.Loan
import tech.figure.loan.v1beta1.LoanOuterClassBuilders.Loan
import tech.figure.loan.v1beta1.app
import tech.figure.loan.v1beta1.borrowers
import tech.figure.loan.v1beta1.interestRate
import tech.figure.loan.v1beta1.principalAmount
import tech.figure.loan.v1beta1.terms
import tech.figure.loan.v1beta1.totalAmount
import tech.figure.util.v1beta1.UUID
import tech.figure.util.v1beta1.addPhoneNumbers
import tech.figure.util.v1beta1.name
import tech.figure.util.v1beta1.primary

class ProtoTest {
    @Test
    fun makeMeAnAsset() {
        val loan = makeALoan()
        val asset = AssetOuterClassBuilders.Asset {
            id = loan.id
            type = "LOAN"
            description = loan.loanType
            putKv("loan", loan.toAny())
        }
        val node = OBJECT_MAPPER.readTree(asset.toJsonString())
        val loanNode = node.path("kv").path("loan") as ObjectNode
        loanNode.put("typeUrl", "type.googleapis.com/tech.figure.loan.v1beta1.Loan")
        println(node.toJsonString())

        writeFile("build/loan.json", loan.toJsonString())
        writeFile("build/asset.json", asset.toJsonString())
    }

    fun makeALoan(): Loan = Loan {
        id = randomTechProtoUuid()
        loanType = "PERSONAL_LOAN"
        originatorName = "VDUB LOAN CO"
        app {
            loanPurpose = "DEBT CONSOLIDATION"
        }
        borrowers {
            primary {
                id = randomTechProtoUuid()
                partyType = "PRIMARY_BORROWER"
                name {
                    firstName = "Jane"
                    lastName = "Doe"
                }
                addPhoneNumbers { number = "867-5309" }
            }
        }
        terms {
            principalAmount {
                amount = 100000.00
                currency = "USD"
            }
            totalAmount {
                amount = 102000.00
                currency = "USD"
            }
            termInMonths = 60
            interestRate { value = 0.05 }
        }
    }

    fun randomTechProtoUuid(): UUID = UUID.newBuilder().setValue(randomUuid()).build()
}

