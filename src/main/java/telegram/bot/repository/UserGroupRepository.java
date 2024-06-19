package telegram.bot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import telegram.bot.enity.UserGroupEntity;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends CrudRepository<UserGroupEntity, Integer> {

    List<UserGroupEntity> findAllByChatId(String chatId);

    @Query("from UserGroupEntity as u where u.groupName = ?1 and u.chatId = ?2 ")
    Optional<UserGroupEntity> findByGroupUserNameAndChatId(String groupUserName, String chatId);

}
