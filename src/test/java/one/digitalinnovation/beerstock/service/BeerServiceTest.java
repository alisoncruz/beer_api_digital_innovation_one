package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.entity.Beer;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedSavedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(expectedSavedBeer)).thenReturn(expectedSavedBeer);

        //then
        BeerDTO createdBeerDTO = beerService.createBeer(expectedBeerDTO);
        //JUnit
        assertEquals(expectedBeerDTO.getId(), createdBeerDTO.getId());
        assertEquals(expectedBeerDTO.getName(), createdBeerDTO.getName());

        //Hamcrest
        assertThat(createdBeerDTO.getId(), is(equalTo(expectedBeerDTO.getId())));
        assertThat(createdBeerDTO.getName(), is(equalTo(expectedBeerDTO.getName())));
        assertThat(createdBeerDTO.getQuantity(), is(equalTo(expectedBeerDTO.getQuantity())));

        assertThat(createdBeerDTO.getQuantity(), is(greaterThan(2)));
    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer duplicatedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(duplicatedBeer));

        //then
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(expectedBeerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        //given
        BeerDTO expectFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectFoundBeerModel = beerMapper.toModel(expectFoundBeerDTO);

        //when
        when(beerRepository.findByName(expectFoundBeerDTO.getName())).thenReturn(Optional.of(expectFoundBeerModel));

        //then
        BeerDTO foundedBeerDTO = beerService.findByName(expectFoundBeerDTO.getName());

        assertThat(foundedBeerDTO.getName(), is(equalTo(expectFoundBeerDTO.getName())));
    }

    @Test
    void whenNotRegisteredBeerNameIsGivenThenThrowAnException() {
        //given
        BeerDTO expectFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        //when
        when(beerRepository.findByName(expectFoundBeerDTO.getName())).thenReturn(Optional.empty());

        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(expectFoundBeerDTO.getName()));
    }

    @Test
    void whenlistBeerIsCalledThenReturnAListOfBeers() {
        //given
        BeerDTO expectFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectFoundBeerDTO);

        //when
        when(beerRepository.findAll()).thenReturn(Collections.singletonList(expectedFoundBeer));

        //then
        List<BeerDTO> beerDTOList = beerService.listAll();
        assertThat(beerDTOList, is(not(empty())));
        assertThat(beerDTOList.get(0), is(expectFoundBeerDTO));
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyListOfBeers() {

        //when
        when(beerRepository.findAll()).thenReturn(Collections.emptyList());

        //then
        List<BeerDTO> beerDTOList = beerService.listAll();
        assertThat(beerDTOList, is(empty()));
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {
        //given
        BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedDeletedFoundBeer = beerMapper.toModel(expectedDeletedBeerDTO);

        //when
        when(beerRepository.findById(expectedDeletedBeerDTO.getId())).thenReturn(Optional.of(expectedDeletedFoundBeer));
        doNothing().when(beerRepository).deleteById(expectedDeletedBeerDTO.getId());

        //then
        beerService.deleteById(expectedDeletedBeerDTO.getId());

        verify(beerRepository, times(1)).findById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).deleteById(expectedDeletedBeerDTO.getId());
    }

    @Test
    void whenExclusionIsCalledWithAnInvalidIDThenThrowBeerNotFoundException() {
        //given
        BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        //when
        when(beerRepository.findById(expectedDeletedBeerDTO.getId())).thenReturn(Optional.empty());

        //given
        assertThrows(BeerNotFoundException.class, () -> beerService.deleteById(expectedDeletedBeerDTO.getId()));
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedFoundBeer));
        when(beerRepository.save(expectedFoundBeer)).thenReturn(expectedFoundBeer);

        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = expectedBeerDTO.getQuantity() + quantityToIncrement;

        //then
        BeerDTO incrementedBeerDTO = beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement, equalTo(incrementedBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterIncrement, lessThan(expectedBeerDTO.getMax()));
    }

    @Test
    void whenIncrementIsGreaterThanMaxThenThrowException() {
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToIncrement = 80;

        assertThrows(BeerStockExceededException
                .class, () -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementAfterSumIsGreaterThanMaxThenThrowException() {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

        //then
        int quantityToIncrement = expectedBeer.getQuantity() + 50;
        assertThrows(BeerStockExceededException.class, () -> beerService
                .increment(expectedBeerDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementIsCalledWithInvalidBeerIDThenThrowException() {
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.empty());

        //then
        assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_BEER_ID, 10));

    }


    @Test
    void whenDecrementIsCalledThenDecrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectFoundedBeer = beerMapper.toModel(expectBeerDTO);

        int quantityToDecrement = 5;
        int expectedQuantityAfterDecrement = 5;

        //when
        when(beerRepository.findById(expectBeerDTO.getId())).thenReturn(Optional.of(expectFoundedBeer));
        when(beerRepository.save(expectFoundedBeer)).thenReturn(expectFoundedBeer);

        //then
        BeerDTO beerDTODecremented = beerService.decrement(expectBeerDTO.getId(), quantityToDecrement);
        assertThat(beerDTODecremented.getQuantity(), is(equalTo(expectedQuantityAfterDecrement)));
        assertThat(beerDTODecremented.getQuantity(), is(greaterThan(0)));

    }

    @Test
    void whenDecrementIsCalledToEmptyStockThenEmptyBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //given
        BeerDTO expectBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectFoundedBeer = beerMapper.toModel(expectBeerDTO);

        int quantityToDecrement = 10;
        int expectedQuantityAfterDecrement = 0;
        //when
        when(beerRepository.findById(expectBeerDTO.getId())).thenReturn(Optional.of(expectFoundedBeer));
        when(beerRepository.save(expectFoundedBeer)).thenReturn(expectFoundedBeer);

        //then
        BeerDTO emptyStockBeerDTO = beerService.decrement(expectBeerDTO.getId(), quantityToDecrement);
        assertThat(emptyStockBeerDTO.getQuantity(), is(equalTo(expectedQuantityAfterDecrement)));

    }

    @Test
    void whenDecrementIsLowerThanZeroZeroThenThrowException() {
        //given
        BeerDTO expectBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectFoundedBeer = beerMapper.toModel(expectBeerDTO);

        int quantityToDecrement = 80;

        //when
        when(beerRepository.findById(expectBeerDTO.getId())).thenReturn(Optional.of(expectFoundedBeer));

        //then
        assertThrows(BeerStockExceededException.class, () -> beerService
                .decrement(expectBeerDTO.getId(), quantityToDecrement));
    }

    @Test
    void whenDecrementIsCalledWithAnInvalidIDThenThrowException() {
        //given
        BeerDTO expectBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        int expectedQunatityToDecrement = 5;

        //when
        when(beerRepository.findById(expectBeerDTO.getId())).thenReturn(Optional.empty());

        //then
        assertThrows(BeerNotFoundException.class, () -> beerService
                .decrement(INVALID_BEER_ID, expectedQunatityToDecrement));
    }

}
