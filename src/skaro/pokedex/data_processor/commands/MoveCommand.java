package skaro.pokedex.data_processor.commands;

import org.eclipse.jetty.util.MultiMap;

import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import skaro.pokedex.core.FlexCache;
import skaro.pokedex.core.FlexCache.CachedResource;
import skaro.pokedex.core.IServiceManager;
import skaro.pokedex.core.ServiceConsumerException;
import skaro.pokedex.core.ServiceType;
import skaro.pokedex.data_processor.AbstractCommand;
import skaro.pokedex.data_processor.IDiscordFormatter;
import skaro.pokedex.data_processor.Response;
import skaro.pokedex.data_processor.TypeData;
import skaro.pokedex.input_processor.Input;
import skaro.pokedex.input_processor.Language;
import skaro.pokedex.input_processor.arguments.ArgumentCategory;
import skaro.pokeflex.api.Endpoint;
import skaro.pokeflex.api.IFlexObject;
import skaro.pokeflex.api.PokeFlexFactory;
import skaro.pokeflex.api.Request;
import skaro.pokeflex.api.RequestURL;
import skaro.pokeflex.objects.move.Move;
import skaro.pokeflex.objects.type.Type;

public class MoveCommand extends AbstractCommand 
{
	public MoveCommand(IServiceManager services, IDiscordFormatter formatter) throws ServiceConsumerException
	{
		super(services, formatter);
		if(!hasExpectedServices(this.services))
			throw new ServiceConsumerException("Did not receive all necessary services");
		
		commandName = "move".intern();
		argCats.add(ArgumentCategory.MOVE);
		expectedArgRange = new ArgumentRange(1,1);
		
		aliases.put("mv", Language.ENGLISH);
		aliases.put("moves", Language.ENGLISH);
		aliases.put("attack", Language.ENGLISH);
		aliases.put("attacke", Language.GERMAN);
		aliases.put("movimiento", Language.SPANISH);
		aliases.put("capacite", Language.FRENCH);
		aliases.put("capacité", Language.FRENCH);
		aliases.put("attaque", Language.FRENCH);
		aliases.put("mossa", Language.ITALIAN);
		aliases.put("waza", Language.JAPANESE_HIR_KAT);
		aliases.put("zhāoshì", Language.CHINESE_SIMPMLIFIED);
		aliases.put("zhaoshi", Language.CHINESE_SIMPMLIFIED);
		aliases.put("gisul", Language.KOREAN);
		
		aliases.put("わざ", Language.JAPANESE_HIR_KAT);
		aliases.put("招式", Language.CHINESE_SIMPMLIFIED);
		aliases.put("기술", Language.KOREAN);
		
		createHelpMessage("Ember", "dragon ascent", "aeroblast", "Blast Burn",
				"https://i.imgur.com/B3VtWyg.gif");
	}
	
	@Override
	public boolean makesWebRequest() { return true; }
	@Override
	public String getArguments() { return "<move>"; }
	
	@Override
	public boolean hasExpectedServices(IServiceManager services) 
	{
		return super.hasExpectedServices(services) &&
				services.hasServices(ServiceType.POKE_FLEX, ServiceType.CACHE);
	}
	
	@Override
	public Mono<Response> discordReply(Input input, User requester)
	{
		if(!input.isValid())
			return Mono.just(formatter.invalidInputResponse(input));
		
		EmbedCreateSpec builder = new EmbedCreateSpec();
		Mono<MultiMap<IFlexObject>> result;
		String moveName = input.getArg(0).getFlexForm();
		
		try
		{
			PokeFlexFactory factory = (PokeFlexFactory)services.getService(ServiceType.POKE_FLEX);
			FlexCache flexCache = (FlexCache)services.getService(ServiceType.CACHE);
			TypeData cachedTypeData = (TypeData)flexCache.getCachedData(CachedResource.TYPE);
			Request initialRequest = new Request(Endpoint.MOVE, moveName);
			
			result = Mono.just(new MultiMap<IFlexObject>())
					.flatMap(dataMap -> initialRequest.makeRequest(factory)
							.ofType(Move.class)
							.doOnNext(move -> {
								dataMap.put(Move.class.getName(), move);
								dataMap.put(Type.class.getName(), cachedTypeData.getByName(move.getType().getName()));
							})
							.flatMap(move -> Flux.just(new RequestURL(move.getDamageClass().getUrl(), Endpoint.MOVE_DAMAGE_CLASS))
									.concatWithValues(new RequestURL(move.getTarget().getUrl(), Endpoint.MOVE_TARGET))
									.concatWithValues(new RequestURL(move.getContestType().getUrl(), Endpoint.CONTEST_TYPE))
									.flatMap(request -> request.makeRequest(factory))
									.doOnNext(flexObject -> dataMap.put(flexObject.getClass().getName(), flexObject))
									.then(Mono.just(dataMap))
									)
							
							
							);
			
			this.addRandomExtraMessage(builder);
			return result.map(dataMap -> formatter.format(input, dataMap, builder));
		}
		catch(Exception e)
		{
			Response response = new Response();
			this.addErrorMessage(response, input, "1006", e); 
			return Mono.just(response);
		}
	}
	
}
