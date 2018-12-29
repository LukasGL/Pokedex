package skaro.pokedex.data_processor.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.MultiMap;

import skaro.pokedex.core.IServiceManager;
import skaro.pokedex.core.ServiceConsumerException;
import skaro.pokedex.core.ServiceType;
import skaro.pokedex.data_processor.AbstractCommand;
import skaro.pokedex.data_processor.IDiscordFormatter;
import skaro.pokedex.data_processor.Response;
import skaro.pokedex.input_processor.Input;
import skaro.pokedex.input_processor.Language;
import skaro.pokedex.input_processor.arguments.ArgumentCategory;
import skaro.pokeflex.api.Endpoint;
import skaro.pokeflex.api.PokeFlexFactory;
import skaro.pokeflex.api.PokeFlexRequest;
import skaro.pokeflex.api.Request;
import skaro.pokeflex.api.RequestURL;
import skaro.pokeflex.objects.ability.Ability;
import skaro.pokeflex.objects.pokemon.Pokemon;
import skaro.pokeflex.objects.pokemon_species.PokemonSpecies;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

public class AbilityCommand extends AbstractCommand 
{	
	public AbilityCommand(IServiceManager services, IDiscordFormatter formatter) throws ServiceConsumerException
	{
		super(services, formatter);
		if(!hasExpectedServices(this.services))
			throw new ServiceConsumerException("Did not receive all necessary services");
		
		commandName = "ability".intern();
		argCats.add(ArgumentCategory.POKE_ABIL);
		expectedArgRange = new ArgumentRange(1,1);
		
		aliases.put("ab", Language.ENGLISH);
		aliases.put("abil", Language.ENGLISH);
		aliases.put("habilidad", Language.SPANISH);
		aliases.put("habil", Language.SPANISH);
		aliases.put("talents", Language.FRENCH);
		aliases.put("talent", Language.FRENCH);
		aliases.put("abilità", Language.ITALIAN);
		aliases.put("abilita", Language.ITALIAN);
		aliases.put("fähigkeiten", Language.GERMAN);
		aliases.put("fahigkeiten", Language.GERMAN);
		aliases.put("tokusei", Language.JAPANESE_HIR_KAT);
		aliases.put("toku", Language.JAPANESE_HIR_KAT);
		aliases.put("tèxìng", Language.CHINESE_SIMPMLIFIED);
		aliases.put("texing", Language.CHINESE_SIMPMLIFIED);
		aliases.put("teugseong", Language.KOREAN);
		
		aliases.put("特性", Language.JAPANESE_HIR_KAT);
		aliases.put("특성", Language.KOREAN);
		aliases.put("特技", Language.CHINESE_SIMPMLIFIED);
		
		createHelpMessage("Starmie", "Flash Fire", "celebi", "natural cure",
				"https://i.imgur.com/biWBKIL.gif");
	}
	
	public boolean makesWebRequest() { return true; }
	public String getArguments() { return "<pokemon> or <ability>"; }
	
	@Override
	public boolean hasExpectedServices(IServiceManager services) 
	{
		return super.hasExpectedServices(services) &&
				services.hasServices(ServiceType.POKE_FLEX, ServiceType.PERK);
	}
	
	public Response discordReply(Input input, IUser requester)
	{
		if(!input.isValid())
			return formatter.invalidInputResponse(input);
		
		MultiMap<Object> dataMap = new MultiMap<Object>();
		EmbedBuilder builder = new EmbedBuilder();
		PokeFlexFactory factory;
		
		try
		{
			factory = (PokeFlexFactory)services.getService(ServiceType.POKE_FLEX);
			
			if(input.getArg(0).getCategory() == ArgumentCategory.ABILITY)
			{
				Object flexObj = factory.createFlexObject(Endpoint.ABILITY, input.argsAsList());
				dataMap.put(Ability.class.getName(), flexObj);
			}
			else//if(input.getArg(0).getCategory() == ArgumentCategory.POKEMON)
			{
				List<PokeFlexRequest> concurrentRequestList = new ArrayList<PokeFlexRequest>();
				List<Object> flexData = new ArrayList<Object>();
				
				//Pokemon
				Pokemon pokemon = (Pokemon)factory.createFlexObject(Endpoint.POKEMON, input.argsAsList());
				dataMap.put(Pokemon.class.getName(), pokemon);
				
				//PokemonSpecies
				Request request = new Request(Endpoint.POKEMON_SPECIES);
				request.addParam(pokemon.getSpecies().getName());
				PokemonSpecies species = (PokemonSpecies)factory.createFlexObject(request);
				dataMap.put(PokemonSpecies.class.getName(), species);
				
				//Abilities
				for(skaro.pokeflex.objects.pokemon.Ability abil : pokemon.getAbilities())
					concurrentRequestList.add(new RequestURL(abil.getAbility().getUrl(), Endpoint.ABILITY));
				
				//Make PokeFlex request
				flexData = factory.createFlexObjects(concurrentRequestList);
				
				//Add all data to the map
				for(Object obj : flexData)
					dataMap.add(obj.getClass().getName(), obj);
				
				this.addAdopter(pokemon, builder);
			}
			
			this.addRandomExtraMessage(builder);
			return formatter.format(input, dataMap, builder);
		}
		catch(Exception e)
		{
			Response response = new Response();;
			this.addErrorMessage(response, input, "1003", e);
			return response;
		}
	}

}