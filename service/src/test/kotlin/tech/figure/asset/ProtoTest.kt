package tech.figure.asset

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.Any
//import io.envoyproxy.pgv.ReflectiveValidatorIndex
//import io.envoyproxy.pgv.ValidatorIndex
import org.junit.jupiter.api.Test
import tech.figure.asset.extensions.toJsonString
import tech.figure.asset.extensions.writeFile
import tech.figure.asset.loan.*
import tech.figure.asset.loan.LoanOuterClassBuilders.Loan
import tech.figure.individual.addPhoneNumbers
import tech.figure.individual.name
import tech.figure.individual.primary
import java.util.*


class ProtoTest {


    @Test
    fun makeMeAnAsset() {
        val loan: Loan = makeALoan()

        val asset = AssetOuterClassBuilders.Asset {
            id = loan.id
            type = "LOAN"
            description = "${loan.meta.loanType} ${loan.meta.loanNumber}"
            putKv("loan", Any.pack(loan, ""))
        }


        val node = OBJECT_MAPPER.readTree(asset.toJsonString())
        val loanNode = node.path("kv").path("loan") as ObjectNode
        loanNode.put("typeUrl", "type.googleapis.com/tech.figure.asset.loan.Loan")
        println(node.toJsonString())

        writeFile("build/loan.json", loan.toJsonString())
        writeFile("build/asset.json", asset.toJsonString())

//        println(asset.toJsonString())
    }

    fun makeALoan(): Loan = Loan {
        id = randomUuid()
        meta {
            loanNumber = "LOAN-1234"
            loanType = "PERSONAL_LOAN"
            loanPurpose = "DEBT CONSOLIDATION"
            originatorName = "VDUB LOAN CO"
        }
        borrowers {
            primary {
                id = randomUuid()
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
            originationFeeRate { value = 0.02 }
        }
        signedPromNote {
            id = randomUuid()
        }
    }

    @Test
    fun test() {

        val loan = Loan {
            id = randomUuid()
            meta {
                loanNumber = "FIGURE-1234"
                loanType = "FIGURE_HELOC"
                loanPurpose = "DEBT CONSOLIDATION"
                originatorName = "VDUB MORTGAGE CO"
            }
            borrowers {
                primary {
                    id = randomUuid()
                    partyType = "PRIMARY_BORROWER"
                    name {
                        firstName = "Valerie"
                        lastName = "Wagner"
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
                originationFeeRate { value = 0.02 }
            }
            signedPromNote {
                id = randomUuid()
            }

        }

        println("Checking loan validation....")
//        val index: ValidatorIndex = ReflectiveValidatorIndex()
//        index.validatorFor<Loan>(loan.javaClass).assertValid(loan)
//        println("Done with loan validation.")


    }
}

fun randomUuid() = UUID.randomUUID().toString()
