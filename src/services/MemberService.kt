package com.linebizplus.exchange.virtual.services

import com.linebizplus.exchange.virtual.components.query
import com.linebizplus.exchange.virtual.entities.BalanceTable
import com.linebizplus.exchange.virtual.entities.MemberTable
import com.linebizplus.exchange.virtual.model.Member
import org.jetbrains.exposed.sql.selectAll

object MemberService {
    private val members = mutableMapOf<Long, Member>()

    /**
     * DB에서 회원정보를 모두 불러온다
     */
    suspend fun initialize() {
        query {
            MemberTable.selectAll().map {
                members[it[MemberTable.id]] =
                    Member(it[MemberTable.id], it[MemberTable.name], it[MemberTable.apiKey])
            }

            BalanceTable.selectAll().map {
                members[it[BalanceTable.memberId]]?.initializeBalance(
                    it[BalanceTable.asset],
                    it[BalanceTable.amount],
                    it[BalanceTable.locked]
                )
            }
        }
    }

    fun getMemberById(id: Long) = members[id]

    fun getMemberByApiKey(apiKey: String) = members.values.find { it.apiKey == apiKey }

    fun addWebSocket(memberId: Long, socketId: String) {
        members[memberId]?.addSocketId(socketId)
    }

    fun removeWebSocket(memberId: Long, socketId: String) {
        members[memberId]?.removeSocketId(socketId)
    }
}
